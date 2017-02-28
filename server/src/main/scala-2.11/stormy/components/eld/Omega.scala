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
    var ranksTopology: TreeMap[Int, NetAddress] = TreeMap[Int, NetAddress](ranks.map(_.swap).toArray:_*)(implicitly[Ordering[Int]].reverse)
    var suspected: Set[NetAddress] = Set()
    var leader: Option[NetAddress] = None
    var neighbors: mutable.Set[NetAddress] = mutable.Set()

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")

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
            logger.debug(s"$self Received topology update: ${t.partitions}. Resetting...")
            ranks = t.ranks
            suspected = Set()
            val lut = t
            neighbors = lut.partitions.find(p => p._2.contains(self)).get._2
            for (p <- neighbors) {
                val neighborRank = ranks(p)
                ranksTopology += (neighborRank -> p)
            }
            if (leader.isEmpty) {
                leader = Some(ranksTopology.head._2)
                logger.debug(s"Updated previously empty leader to $leader.get")
                trigger(Trust(leader.get) -> eld)
            }
        }
    }

    epfd uponEvent {
        case Suspect(p: NetAddress) => handle {
            suspected = suspected union Set(p)
            leaderCheck()
        }
        case Restore(p: NetAddress) => handle {
            suspected = suspected - p
            leaderCheck()
        }
    }

    private def leaderCheck(): Unit = {
        val newLeader: Option[NetAddress] = Some(ranksTopology.filter( rankNodePair =>
            !suspected.contains(rankNodePair._2)
        ).head._2)
        if (newLeader.isEmpty) {
            logger.warn("No new leader available. Assigning self as leader.")
            leader = Some(self)
        } else if (leader.isEmpty || leader.get != newLeader.get) {
            leader = newLeader
            logger.info(s"$self New leader chosen: $newLeader")
            trigger(Trust(leader.get) -> eld)
        }
    }
}
