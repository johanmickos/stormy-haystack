package components.epfd

import components.Ports.{PL_Deliver, PL_Send, PerfectLink}
import ex.TAddress
import se.sics.kompics.Start
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timer}

class EPFD(epfdInit: Init[EPFD]) extends ComponentDefinition {

    //EPFD subscriptions
    val timer = requires[Timer]
    val pLink = requires[PerfectLink]
    val epfd = provides[EventuallyPerfectFailureDetector]

    // EPDF component state and initialization

    // TODO
    //configuration parameters
    val self = epfdInit match {case Init(s: TAddress) => s}
    val topology = cfg.getValue[List[TAddress]]("components.epfd.simulation.topology")
    val delta = cfg.getValue[Long]("components.epfd.simulation.delay")

    //mutable state
    var period = cfg.getValue[Long]("components.epfd.simulation.delay")
    var alive = Set(cfg.getValue[List[TAddress]]("components.epfd.simulation.topology"): _*)
    var suspected = Set[TAddress]()
    var seqnum = 0

    def startTimer(delay: Long): Unit = {
        val scheduledTimeout = new ScheduleTimeout(period)
        scheduledTimeout.setTimeoutEvent(CheckTimeout(scheduledTimeout))
        trigger(scheduledTimeout -> timer)
    }

    ctrl uponEvent {
        case _: Start => handle {

            /* WRITE YOUR CODE HERE  */
            startTimer(period)

        }
    }

    timer uponEvent {
        case CheckTimeout(_) => handle {
            if (alive.intersect(suspected).nonEmpty) {

                /* WRITE YOUR CODE HERE  */
                period += delta

            }

            seqnum = seqnum + 1

            for (p <- topology) {
                if (!alive.contains(p) && !suspected.contains(p)) {

                    /* WRITE YOUR CODE HERE  */
                    suspected = suspected + p

                } else if (alive.contains(p) && suspected.contains(p)) {
                    suspected = suspected - p
                    trigger(Restore(p) -> epfd)
                }
                trigger(PL_Send(p, HeartbeatRequest(seqnum)) -> pLink)
            }
            alive = Set[TAddress]()
            startTimer(period)
        }
    }

    // From 'requires PerfectLink'
    pLink uponEvent {
        case PL_Deliver(src, HeartbeatRequest(seq)) => handle {

            /* WRITE YOUR CODE HERE  */
            trigger(PL_Send(src, HeartbeatReply(seqnum)) -> pLink)

        }
        case PL_Deliver(src, HeartbeatReply(seq)) => handle {
            if (seq == seqnum || suspected.contains(src)) {
                alive = alive + src
            }

            /* WRITE YOUR CODE HERE  */

        }
    }
}