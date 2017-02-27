package stormy.components.consensus

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import stormy.components.Ports._
import stormy.components.consensus.ASCMessages._
import stormy.components.epfd.EPDFSpec.{EventuallyPerfectFailureDetector, Suspect}
import stormy.networking.{NetAddress, NetMessage}
import stormy.overlay.{OverlayUpdate, PartitionLookupTable, Routing}

import scala.collection.mutable

// Implements AbortableSequenceConsensus
class ASC(init: Init[ASC]) extends ComponentDefinition with StrictLogging {

    def this() {
        this(Init[ASC](-1, Set[NetAddress]()))
    }

    val asc = provides[AbortableConsensus]

    val fpl = requires[Network]
    val routing = requires[Routing]
    val epfd = requires[EventuallyPerfectFailureDetector]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    // TODO Determine what rank is (from book)
    private var (rank: Int, topology: Set[NetAddress]) = init match {
        case Init(r: Int, t: Set[NetAddress]@unchecked) => (r, t)
    }

    private var N: Int = topology.size

    private var t: Int = 0

    private var prepts: Int = 0
    private var ats: Int = 0
    private var av: List[Any] = List()
    private var al: Int = 0

    private var pts: Int = 0
    private var pv: List[Any] = List()
    private var pl: Int = 0
    private var proposedValues: List[Any] = List()

    private var readList: mutable.HashMap[NetAddress, Any] = mutable.HashMap()

    private var accepted: mutable.HashMap[NetAddress, Int] = mutable.HashMap()
    private var decided: mutable.HashMap[NetAddress, Int] = mutable.HashMap()


    ctrl uponEvent {
        case _: Start => handle {
            logger.info("Starting abortable sequence consensus")
        }
    }

    def prefix(av: List[Any], al: Int): List[Any] = {
        av.take(al)
    }

    def suffix(av: List[Any], l: Int): List[Any] = {
        av.drop(l)
    }

    routing uponEvent {
        case OverlayUpdate(lut: PartitionLookupTable) => handle {
            logger.debug(s"$self Received topology update: $lut. Resetting...")
            val neighbors = lut.partitions.find(p => p._2.contains(self)).get._2
            rank = lut.ranks(self)
            topology = Set() ++ neighbors
            N = topology.size
        }
    }

    asc uponEvent {
        // Prepare phase
        case AC_Propose(v) => handle {
            logger.debug(s"$self handling AC_Propose for $v")

            t = t + 1
            if (pts == 0) {
                pts = t * N + rank
                pv = prefix(av, al)
                pl = 0
                proposedValues = List(v)
                readList = mutable.HashMap()
                accepted = mutable.HashMap()
                decided = mutable.HashMap()
                for (p <- topology) {
                    trigger(NetMessage(self, p, PrepareMessage(pts, al, t)) -> fpl)
                }
            } else if (readList.size <= (N / 2).floor) {
                proposedValues = proposedValues :+ v
            } else if (!pv.contains(v)) {
                pv = pv :+ v
                for (p <- topology) {
                    trigger(NetMessage(self, p, AcceptMessage(pts, List(v), pv.size - 1, t)) -> fpl)
                }
            }
        }
    }

    fpl uponEvent {
        // Prepare phase
        case NetMessage(source, `self`, PrepareMessage(ts, l, tPrime)) => handle {
            logger.debug(s"$self handling AC_Prepare for $ts $l $tPrime")

            t = math.max(t, tPrime) + 1
            if (ts < prepts) {
                trigger(NetMessage(self, source, NackMessage(ts, t)) -> fpl)
            } else {
                prepts = ts
                val sfx: List[Any] = suffix(av, l)
                logger.debug(s"Sending PrepareAckMessage with suffix $sfx")
                trigger(NetMessage(self, source, PrepareAckMessage(ts, ats, sfx, al, t)) -> fpl)
            }
        }
        case NetMessage(_, `self`, NackMessage(ptsPrime, tPrime)) => handle {
            logger.debug(s"$self handling AC_Nack for $ptsPrime $tPrime")

            t = math.max(t, tPrime) + 1
            if (ptsPrime == pts) {
                pts = 0
                trigger(AC_Abort -> asc)
            }
        }
        // Accept phase
        case NetMessage(source, `self`, PrepareAckMessage(ptsPrime, ts, vsuffix, l, tPrime)) => handle {
            logger.debug(s"$self handling AC_PrepareAck for $ptsPrime, $ts, $vsuffix, $l, $tPrime")

            t = math.max(t, tPrime) + 1
            if (ptsPrime == pts) {
                readList(source) = (ts, vsuffix)
                decided(source) = l
                if (readList.size == ((N / 2).floor + 1)) {
                    var (tsprime: Int, vsuffixPrime: List[Any]) = (0, List[Any]())
                    // TODO Is there a cleaner way to express the if-statement below?
                    // TODO Right now it's just copy-pasta from the PDF
                    for ((tsPrimePrime: Int, vsuffixPrimePrime: List[Any]@unchecked) <- readList.values) {
                        if ((tsprime < tsPrimePrime) || (tsprime == tsPrimePrime && vsuffixPrime.size < vsuffixPrimePrime.size)) {
                            tsprime = tsPrimePrime
                            vsuffixPrime = vsuffixPrimePrime
                        }
                    }
                    pv = pv ++ vsuffixPrime
                    for (v <- proposedValues if !pv.contains(v)) {
                        pv = pv :+ v
                    }
                    for (p <- topology if readList.contains(p)) {
                        val lPrime = decided(p)
                        val sfx = suffix(pv, lPrime)
                        logger.debug(s"Sending AcceptMessage with suffix $sfx")
                        trigger(NetMessage(self, p, AcceptMessage(pts, sfx, lPrime, t)) -> fpl)
                    }
                } else if (readList.size > ((N / 2).floor + 1)) {
                    trigger(NetMessage(self, source, AcceptMessage(pts, suffix(pv, l), l, t)) -> fpl)
                    if (pl != 0) {
                        trigger(NetMessage(self, source, AC_Decide(pts, pl, t)) -> fpl)
                    }
                }
            }
        }
        case NetMessage(source, `self`, AcceptMessage(ts, vsuffix, offs, tPrime)) => handle {
            t = math.max(t, tPrime) + 1
            if (ts != prepts) {
                trigger(NetMessage(self, source, NackMessage(ts, t)) -> fpl)
            } else {
                ats = ts
                if (offs < av.size) {
                    av = prefix(av, offs)
                }
                av = av ++ vsuffix
                trigger(NetMessage(self, source, AcceptAckMessage(ts, av.size, t)) -> fpl)
            }
        }
        case NetMessage(source, `self`, AcceptAckMessage(ptsPrime, l, tPrime)) => handle {
            t = math.max(t, tPrime) + 1
            if (ptsPrime == pts) {
                accepted(source) = l
                // TODO Is there a cleaner way to express this if-statement?
                // if pl < l ∧ #({p ∈ Π | accepted[p] ≥ l}) > bN/2c then
                val acceptors = for (p <- topology if accepted.contains(p) && accepted(p) >= l) yield p
                if ((pl < l) && (acceptors.size > (N / 2).floor)) {
                    pl = l
                    for (p <- topology if Some(readList(p)).isDefined) {
                        trigger(NetMessage(self, p, DecideMessage(pts, pl, t)) -> fpl)
                    }
                }
            }
        }
        case NetMessage(source, self, DecideMessage(ts, l, tPrime)) => handle {
            t = math.max(t, tPrime) + 1
            if (ts == prepts) {
                while (al < l) {
                    trigger(AC_Decide(av(al)) -> asc)
                    al = al + 1
                }
            }
        }
    }

    epfd uponEvent {
        case Suspect(node) => handle {
            // TODO Fix this when implementing reconfiguration
            topology = topology - node
            readList.remove(node)
            accepted.remove(node)
            decided.remove(node)
            N = topology.size
            logger.info(s"$self removing $node from topology")
        }
    }
}







