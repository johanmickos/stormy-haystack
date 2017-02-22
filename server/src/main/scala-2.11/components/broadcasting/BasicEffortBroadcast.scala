package components.broadcasting

import components.Ports._
import networking.NetAddress
import se.sics.kompics.sl._

class BasicEffortBroadcast(init: Init[BasicEffortBroadcast]) extends ComponentDefinition {

    val pLink = requires[PerfectLink]
    val beb = provides[BestEffortBroadcast]

    val (self, topology) = init match {
        case Init(s: NetAddress, t: Set[NetAddress]@unchecked) => (s, t)
    }

    beb uponEvent {
        case x: BEB_Broadcast => handle {
            for (q <- topology) {
                trigger(PL_Send(q, x) -> pLink)
            }
        }
    }

    pLink uponEvent {
        case PL_Deliver(src, BEB_Broadcast(payload)) => handle {
            trigger(BEB_Deliver(src, payload) -> beb)
        }
    }
}