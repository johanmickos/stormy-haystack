package stormy.components.eld

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Start
import se.sics.kompics.sl._
import stormy.components.eld.ELDSpec.{EventualLeaderDetector, Trust}
import stormy.components.epfd.EPDFSpec.{EventuallyPerfectFailureDetector, Restore, Suspect}
import stormy.networking.NetAddress
import stormy.overlay.{OverlayUpdate, PartitionLookupTable, Routing}

import scala.collection.immutable.TreeMap
import scala.collection.mutable

class Omega(init: Init[Omega]) extends ComponentDefinition with StrictLogging {

    def this() {
        this(Init[Omega](mutable.HashMap[NetAddress, Int]()))
    }


    val eld = provides[EventualLeaderDetector]

    val epfd = requires[EventuallyPerfectFailureDetector]
    val routing = requires[Routing]


    var ranks: mutable.HashMap[NetAddress, Int] = init match {
        case Init(nodes: mutable.HashMap[NetAddress, Int]@unchecked) => nodes
    }
    /**
      * Stores the current topology sorted according to the nodes' ranks
      */
    var ranksTopology: TreeMap[Int, NetAddress] = TreeMap[Int, NetAddress](ranks.map(_.swap).toArray:_*)
    var suspected: Set[NetAddress] = Set()
    var leader: Option[NetAddress] = None

    ctrl uponEvent {
        case _: Start => handle {
            logger.info("Starting Omega")
            if (ranksTopology.nonEmpty) {
                leader = Some(ranksTopology.head._2)
                trigger(Trust(leader.get) -> eld)
            }
        }
    }

    routing uponEvent {
        case OverlayUpdate(t: PartitionLookupTable) => handle {
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
        val newLeader = ranksTopology.filter( rankNodePair =>
            !suspected.contains(rankNodePair._2)
        ).head._2

        if (leader.isEmpty || leader.get != newLeader) {
            leader = Some(newLeader)
            logger.info(s"New leader chosen: $newLeader")
            trigger(Trust(leader.get) -> eld)
        }
    }
}
