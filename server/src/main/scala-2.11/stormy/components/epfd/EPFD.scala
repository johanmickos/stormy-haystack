package stormy.components.epfd

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timer}
import stormy.networking.{NetAddress, NetMessage}
import stormy.overlay.{OverlayUpdate, Routing}

class EPFD(init: Init[EPFD]) extends ComponentDefinition with StrictLogging {

    def this() {
        this(Init(Set[NetAddress]()))
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
            if (bootType.toLowerCase().equals("coordinator")) {
                startTimer(period)
            }
            logger.info("Starting EPFD")
        }
    }

    routing uponEvent {
        case OverlayUpdate(t: Iterable[NetAddress]) => handle {
            logger.debug(s"Received topology update: $t. Resetting...")
            topology = t.toSet
            alive = Set() ++ topology
            suspected = Set[NetAddress]()
            seqnum = 0 // TODO is this safe?
            period = cfg.getValue[Long]("stormy.components.epfd.delay")
        }
        case whatever => handle {
            logger.info(s"Received unknown routing event: $whatever")
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
                    logger.info(s"Suspecting $p")
                    trigger(Suspect(p) -> epfd)
                } else if (alive.contains(p) && suspected.contains(p)) {
                    suspected = suspected - p
                    logger.info(s"Restoring $p")
                    trigger(Restore(p) -> epfd)
                }
                trigger(NetMessage(self, p, HeartbeatRequest(seqnum)) -> network)
            }
            alive = Set[NetAddress]()
            startTimer(period)
        }
        case whatever => handle {
            logger.warn(s"Unknown timer event: $whatever")
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