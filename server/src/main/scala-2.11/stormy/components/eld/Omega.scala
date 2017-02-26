package stormy.components.eld

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Start
import se.sics.kompics.sl._
import stormy.components.eld.ELDSpec.{EventualLeaderDetector, Trust}
import stormy.components.epfd.EPDFSpec.{EventuallyPerfectFailureDetector, Restore, Suspect}
import stormy.networking.NetAddress
import stormy.overlay.{OverlayUpdate, Routing}

import scala.collection.immutable.TreeMap

class Omega(init: Init[Omega]) extends ComponentDefinition with StrictLogging {

    def this() {
        this(Init[Omega](TreeMap[NetAddress, Int]()))
    }


    val eld = provides[EventualLeaderDetector]

    val epfd = requires[EventuallyPerfectFailureDetector]
    val routing = requires[Routing]

    // TODO Handle changes in topology
    var topology: TreeMap[NetAddress, Int] = init match {
        case Init(nodes: TreeMap[NetAddress, Int]@unchecked) => nodes
    }
    var suspected: Set[NetAddress] = Set()
    var leader: Option[NetAddress] = None

    ctrl uponEvent {
        case _: Start => handle {
            logger.info("Starting Omega")
            if (topology.nonEmpty) {
                leader = Some(topology.head._1)
                trigger(Trust(leader.get) -> eld)
            }
        }
    }

    routing uponEvent {
        case OverlayUpdate(t: Iterable[NetAddress]) => handle {
            logger.debug(s"Received topology update: $t. Resetting...")
        }
    }

    epfd uponEvent {
        case Suspect(p: NetAddress) => handle {
            logger.info(s"Received suspected process $p")
            suspected = suspected union Set(p)
            leaderCheck()
        }
        case Restore(p: NetAddress) => handle {
            logger.info(s"Received suspected process $p")
            suspected = suspected - p
            leaderCheck()
        }
    }

    private def leaderCheck(): Unit = {
        val newLeader: NetAddress = (topology -- suspected).head._1
        if (leader.isEmpty || leader.get != newLeader) {
            leader = Some(newLeader)
            logger.info(s"New leader chosen: $newLeader")
            trigger(Trust(leader.get) -> eld)
        }
    }
}
