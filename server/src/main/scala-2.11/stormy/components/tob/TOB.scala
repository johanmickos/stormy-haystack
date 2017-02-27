package stormy.components.tob

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.{SchedulePeriodicTimeout, Timeout, Timer}
import stormy.components.Ports.{AC_Abort, AC_Decide, AC_Propose, AbortableConsensus}
import stormy.components.eld.ELDSpec.{EventualLeaderDetector, Trust}
import stormy.components.epfd.EPDFSpec.EventuallyPerfectFailureDetector
import stormy.components.tob.TOBSpec.{TOB_Broadcast, TOB_Deliver, TotalOrderBroadcast}
import stormy.kv.{Operation, WrappedOperation}
import stormy.networking.{NetAddress, NetMessage}

class TOB extends ComponentDefinition with StrictLogging {
    val tob = provides[TotalOrderBroadcast]

    val timer = requires[Timer]
    val pLink = requires[Network]
    val omega = requires[EventualLeaderDetector]
    val epfd = requires[EventuallyPerfectFailureDetector]
    val asc = requires[AbortableConsensus]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    var leader: NetAddress = self

    val timeout: Long = cfg.getValue[Long]("stormy.components.tob.broadcastTimeout")
    private var timeouts: Set[String] = Set()


    var unordered: Set[WrappedOperation] = Set()

    timer uponEvent {
        case BroadcastTimeout(_) => handle {
            // TODO Adjust timeout interval to account for increases/decreases in request volume
            logger.debug(s"$self received broadcast timeout. Sending all unordered messages $unordered")
            for (m <- unordered) {
                trigger(NetMessage(self, leader, m) -> pLink)
            }
        }
    }
    tob uponEvent {
        case TOB_Broadcast(op: WrappedOperation) => handle {
            logger.debug(s"$self TOB_Broadcast for $op")

            unordered = unordered + op

            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(BroadcastTimeout(spt))
            trigger(spt -> timer)
            // TODO Kill all timeouts when shutting down
            timeouts = timeouts + spt.getTimeoutEvent.getTimeoutId.toString
            trigger(NetMessage(self, leader, op) -> pLink)
        }
    }

    pLink uponEvent {
        case NetMessage(source, self, op: WrappedOperation) => handle {
            if (trusted()) {
                logger.debug(s"$self Triggering AC_Propose for $op")

                trigger(AC_Propose(op) -> asc)
            }
        }
    }

    omega uponEvent {
        case Trust(node: NetAddress) => handle {
            logger.debug(s"$self Updating leader to $node")

            leader  = node
        }
    }

    asc uponEvent {
        case AC_Abort => handle {
            // Pass
        }
        case AC_Decide(op: WrappedOperation) => handle {
            logger.debug(s"$self AC_Decision made for $op")

            unordered = unordered - op
            trigger(TOB_Deliver(self, op) -> tob)
        }
    }

    private def trusted(): Boolean = {
        self == leader
    }
}

case class BroadcastTimeout(sbt: SchedulePeriodicTimeout) extends Timeout(sbt)
