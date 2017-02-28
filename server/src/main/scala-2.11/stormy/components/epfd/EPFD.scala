package stormy.components.epfd

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timer}
import stormy.components.epfd.EPDFSpec._
import stormy.networking.{NetAddress, NetMessage}
import stormy.overlay.{OverlayUpdate, PartitionLookupTable, Routing}

class EPFD(init: Init[EPFD]) extends ComponentDefinition with StrictLogging {

    def this() {
        this(Init[EPFD](Set[NetAddress]()))
    }

    val timer = requires[Timer]
    val network = requires[Network]
    val routing = requires[Routing]
    val epfd = provides[EventuallyPerfectFailureDetector]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    val bootType: String = cfg.getValue[String]("stormy.type")
    var topology: Set[NetAddress] = init match {
        case Init(nodes: Set[NetAddress] @unchecked) => nodes
    }

    var delta = cfg.getValue[Long]("stormy.components.epfd.delta")
    var period = cfg.getValue[Long]("stormy.components.epfd.delay")
    var alive: Set[NetAddress] = Set() ++ topology
    var suspected = Set[NetAddress]()
    var seqnum = 0

    def startTimer(delay: Long): Unit = {
        val scheduledTimeout = new ScheduleTimeout(period)
        scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout))
        trigger(scheduledTimeout -> timer)
    }

    ctrl uponEvent {
        case _: Start => handle {
            startTimer(period)
            logger.info("Starting EPFD")
        }
    }

    routing uponEvent {
        case OverlayUpdate(lut: PartitionLookupTable) => handle {
            logger.debug(s"Received topology update: $lut. Resetting...")
            topology = lut.getNodes.toSet
            alive = Set() ++ topology
            suspected = Set[NetAddress]()
            seqnum = 0 // TODO is this safe?
            period = cfg.getValue[Long]("stormy.components.epfd.delay")
        }
    }

    timer uponEvent {
        case CheckTimeout(_) => handle {
            if (alive.intersect(suspected).nonEmpty) {
                period += delta
            }
            seqnum = seqnum + 1
            for (p <- topology) {
                if (!alive.contains(p) && !suspected.contains(p)) {
                    suspected = suspected + p
                    logger.debug(s"Suspecting $p")
                    trigger(Suspect(p) -> epfd)
                } else if (alive.contains(p) && suspected.contains(p)) {
                    suspected = suspected - p
                    logger.debug(s"Restoring $p")
                    trigger(Restore(p) -> epfd)
                }
                trigger(NetMessage(self, p, HeartbeatRequest(seqnum)) -> network)
            }
            alive = Set[NetAddress]()
            startTimer(period)
        }
    }

    network uponEvent {
        case NetMessage(src, `self`, HeartbeatRequest(seq)) => handle {
            trigger(NetMessage(self, src, HeartbeatReply(seq)) -> network)
        }
        case NetMessage(src, `self`, HeartbeatReply(seq)) => handle {
            if (seq == seqnum || suspected.contains(src)) {
                alive = alive + src
            }
        }
    }

}