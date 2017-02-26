package stormy.components.tob

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.{SchedulePeriodicTimeout, Timeout, Timer}
import stormy.components.Ports.{AC_Abort, AC_Decide, AC_Propose, AbortableConsensus}
import stormy.components.eld.ELDSpec.{EventualLeaderDetector, Trust}
import stormy.components.epfd.EPDFSpec.EventuallyPerfectFailureDetector
import stormy.components.tob.TOBSpec.{TOB_Broadcast, TOB_Deliver, TotalOrderBroadcast}
import stormy.kv.Operation
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

    val timeout: Long = cfg.getValue[Long]("stormy.keepAlivePeriod") * 2
    private var timeouts: Set[String] = Set()


    var unordered: Set[Operation] = Set()

    timer uponEvent {
        case BroadcastTimeout(op: Operation, _) => handle {
            for (m <- unordered) {
                trigger(NetMessage(self, leader, m) -> pLink)
                val spt = new SchedulePeriodicTimeout(timeout, timeout)
                spt.setTimeoutEvent(BroadcastTimeout(op, spt))
                trigger(spt -> timer)
                // TODO Kill all timeouts when shutting down
                timeouts = timeouts + spt.getTimeoutEvent.getTimeoutId.toString
            }
        }
    }
    tob uponEvent {
        case TOB_Broadcast(op: Operation) => handle {
            unordered = unordered + op

            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(BroadcastTimeout(op, spt))
            trigger(spt -> timer)
            // TODO Kill all timeouts when shutting down
            timeouts = timeouts + spt.getTimeoutEvent.getTimeoutId.toString
            trigger(NetMessage(self, leader, op) -> pLink)
        }
    }

    pLink uponEvent {
        case NetMessage(source, self, op: Operation) => handle {
            if (trusted()) {
                trigger(AC_Propose(op) -> asc)
            }
        }
    }

    omega uponEvent {
        case Trust(node: NetAddress) => handle {
            leader  = node
        }
    }

    asc uponEvent {
        case AC_Abort => handle {
            // Pass
        }
        case AC_Decide(op: Operation) => handle {
            unordered = unordered - op
            // TODO Kill timeout
            trigger(TOB_Deliver(self, op) -> tob)
        }
    }

    private def trusted(): Boolean = {
        self == leader
    }
}

case class BroadcastTimeout(m: Operation, sbt: SchedulePeriodicTimeout) extends Timeout(sbt)