package components

import components.Ports._
import networking.{NetAddress, NetMessage}
import se.sics.kompics.network.Network
import se.sics.kompics.sl._

object Primitives {
    import components.Primitives.PerfectP2PLink._

    object PerfectP2PLink {

        val PerfectLinkMessage = NetMessage
    }

    class PerfectP2PLink(init: Init[PerfectP2PLink]) extends ComponentDefinition {

        val pLink = provides[PerfectLink]
        val network = requires[Network]

        val self: NetAddress = init match {
            case Init(self: NetAddress) => self
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

    case class VectorClock(var vc: Map[NetAddress, Int]) {

        def inc(addr: NetAddress) = {
            vc = vc + ((addr, vc.get(addr).get + 1))
        }

        def set(addr: NetAddress, value: Int) = {
            vc = vc + ((addr, value))
        }

        def <=(that: VectorClock): Boolean = vc.foldLeft[Boolean](true)((leq, entry) => leq & (entry._2 <= that.vc.getOrElse(entry._1, entry._2)))

    }

    object VectorClock {

        def empty(topology: scala.Seq[NetAddress]): VectorClock = {
            VectorClock(topology.foldLeft[Map[NetAddress, Int]](Map[NetAddress, Int]())((mp, addr) => mp + ((addr, 0))))
        }

        def apply(that: VectorClock): VectorClock = {
            VectorClock(that.vc)
        }

    }
}