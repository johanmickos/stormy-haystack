package components.broadcasting

import components.Ports._
import networking.TAddress
import se.sics.kompics.sl._
import se.sics.kompics.KompicsEvent

case class OriginatedData(src: TAddress, payload: KompicsEvent) extends KompicsEvent

class EagerReliableBroadcast(init: Init[EagerReliableBroadcast]) extends ComponentDefinition {

    //EagerReliableBroadcast Subscriptions
    val beb = requires[BestEffortBroadcast]
    val rb = provides[ReliableBroadcast]

    //EagerReliableBroadcast Component State and Initialization
    val self = init match {
        case Init(s: TAddress) => s
    }
    var delivered = collection.mutable.Set[KompicsEvent]()

    //EagerReliableBroadcast Event Handlers
    rb uponEvent {
        case x@RB_Broadcast(payload) => handle {

            /* WRITE YOUR CODE HERE  */
            //  println(s"RB Broadcast at $self \n\twith payload $payload\n\t$x")l

            trigger(BEB_Broadcast(new OriginatedData(self, payload)) -> beb)

        }
    }

    beb uponEvent {
        case BEB_Deliver(_, data@OriginatedData(origin, payload)) => handle {

            /* WRITE YOUR CODE HERE  */
            if (!delivered.contains(payload)) {
                delivered = delivered + payload
                trigger(RB_Deliver(origin, payload) -> rb)
                trigger(BEB_Broadcast(data) -> beb)
            }
        }
    }
}