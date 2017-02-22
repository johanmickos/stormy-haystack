package components.broadcasting

import components.Ports._
import ex.TAddress
import se.sics.kompics.sl._

class BasicBroadcast(init: Init[BasicBroadcast]) extends ComponentDefinition {

    //subscriptions
    val pLink = requires[PerfectLink]
    val beb = provides[BestEffortBroadcast]

    //configuration
    val (self, topology) = init match {
        case Init(s: TAddress, t: Set[TAddress]@unchecked) => (s, t)
    }

    //handlers
    beb uponEvent {
        case x: BEB_Broadcast => handle {

            /* WRITE YOUR CODE HERE  */
            for (q <- topology) {
                trigger(PL_Send(q, x) -> pLink)
            }

        }
    }

    pLink uponEvent {
        case PL_Deliver(src, BEB_Broadcast(payload)) => handle {

            /* WRITE YOUR CODE HERE  */
            trigger(BEB_Deliver(src, payload) -> beb)

        }
    }
}