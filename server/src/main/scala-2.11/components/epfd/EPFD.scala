package components.epfd

import com.typesafe.scalalogging.StrictLogging
import components.Ports.{PL_Deliver, PL_Send, PerfectLink}
import networking.NetAddress
import se.sics.kompics.Start
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timer}

class EPFD(epfdInit: Init[EPFD]) extends ComponentDefinition with StrictLogging {

    val timer = requires[Timer]
    val pLink = requires[PerfectLink]
    val epfd = provides[EventuallyPerfectFailureDetector]

    // TODO
    //configuration parameters
    val (self, topology) = epfdInit match {
        case Init(s: NetAddress, t: Set[NetAddress]) => (s, t)
    }

    //    val topology = cfg.getValue[List[TAddress]]("components.epfd.simulation.topology")
    val delta = cfg.getValue[Long]("components.epfd.simulation.delay")

    var period = cfg.getValue[Long]("components.epfd.simulation.delay")
    var alive = Set(cfg.getValue[List[NetAddress]]("components.epfd.simulation.topology"): _*)
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
                trigger(PL_Send(p, HeartbeatRequest(seqnum)) -> pLink)
            }
            alive = Set[NetAddress]()
            startTimer(period)
        }
    }

    pLink uponEvent {
        case PL_Deliver(src, HeartbeatRequest(seq)) => handle {
            trigger(PL_Send(src, HeartbeatReply(seq)) -> pLink)
        }
        case PL_Deliver(src, HeartbeatReply(seq)) => handle {
            if (seq == seqnum || suspected.contains(src)) {
                alive = alive + src
            }
        }
    }

}