package components.broadcasting

import components.Ports._
import components.Primitives.VectorClock
import ex.TAddress
import se.sics.kompics.sl.{Init, _}
import se.sics.kompics.{KompicsEvent, ComponentDefinition => _, Port => _}

import scala.collection.immutable.Set
import scala.collection.mutable.ListBuffer

//Causal Reliable Broadcast

case class DataMessage(timestamp: VectorClock, payload: KompicsEvent) extends KompicsEvent

class WaitingCRB(init: Init[WaitingCRB]) extends ComponentDefinition {

    //subscriptions
    val rb = requires[ReliableBroadcast]
    val crb = provides[CausalOrderReliableBroadcast]

    //configuration
    val (self, vec) = init match {
        case Init(s: TAddress, t: Set[TAddress]@unchecked) => (s, VectorClock.empty(t.toSeq))
    }

    //state
    var pending: ListBuffer[(TAddress, DataMessage)] = ListBuffer()
    var lsn = 0


    //handlers
    crb uponEvent {
        case ctx@CRB_Broadcast(payload) => handle {
            val w = VectorClock.apply(vec)
            w.set(self, lsn)
            lsn = lsn + 1
            trigger(RB_Broadcast(DataMessage(w, payload)) -> rb)
        }
    }

    rb uponEvent {
        case x@RB_Deliver(src: TAddress, msg: DataMessage) => handle {
            pending += ((src, msg))
            for (it@(address, msg) <- pending.sortWith(_._2.timestamp <= _._2.timestamp) if msg.timestamp <= vec) {
                pending = pending - it
                vec.inc(address)
                trigger(CRB_Deliver(address, msg.payload) -> crb)
            }
        }
    }

}