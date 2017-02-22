package components

import components.Ports._
import networking.{TAddress, TMessage}
import se.sics.kompics.network.Network
import se.sics.kompics.sl._

object Primitives {
    import components.Primitives.PerfectP2PLink._

    object PerfectP2PLink {

        val PerfectLinkMessage = TMessage
    }

    class PerfectP2PLink(init: Init[PerfectP2PLink]) extends ComponentDefinition {

        val pLink = provides[PerfectLink]
        val network = requires[Network]

        val self: TAddress = init match {
            case Init(self: TAddress) => self
        }

        pLink uponEvent {
            case PL_Send(dest, payload) => handle {
                trigger(PerfectLinkMessage(self, dest, payload) -> network)
            }
        }

        network uponEvent {
            case PerfectLinkMessage(src, dest, payload) => handle {
                trigger(PL_Deliver(src, payload) -> pLink)
            }
        }

    }

    case class VectorClock(var vc: Map[TAddress, Int]) {

        def inc(addr: TAddress) = {
            vc = vc + ((addr, vc.get(addr).get + 1))
        }

        def set(addr: TAddress, value: Int) = {
            vc = vc + ((addr, value))
        }

        def <=(that: VectorClock): Boolean = vc.foldLeft[Boolean](true)((leq, entry) => leq & (entry._2 <= that.vc.getOrElse(entry._1, entry._2)))

    }

    object VectorClock {

        def empty(topology: scala.Seq[TAddress]): VectorClock = {
            VectorClock(topology.foldLeft[Map[TAddress, Int]](Map[TAddress, Int]())((mp, addr) => mp + ((addr, 0))))
        }

        def apply(that: VectorClock): VectorClock = {
            VectorClock(that.vc)
        }

    }
}