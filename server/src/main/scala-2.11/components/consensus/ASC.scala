package components.consensus

import com.typesafe.scalalogging.StrictLogging
import components.Ports._
import components.consensus.ASCMessages._
import networking.{NetAddress, NetMessage}
import se.sics.kompics.sl._
import se.sics.kompics.{KompicsEvent, Start}
import se.sics.kompics.network.Network

import scala.collection.mutable
import scala.collection.parallel.immutable

// Implements AbortableSequenceConsensus
class ASC(init: Init[ASC]) extends ComponentDefinition with StrictLogging {

    val asc = provides[AbortableConsensus]
    val fpl = requires[Network]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    // TODO Determine what rank is (from book)
    private val (rank: Int, topology: mutable.Set[NetAddress]) = init match {
        case Init(r: Int, t: mutable.Set[NetAddress] @unchecked) => (r, t)
    }

    private val N: Int = topology.size

    // TODO We could abstract away logical clock into own type
    private var t: Int = 0

    private var prepts: Int = 0
    private var ats: Int = 0
    private var av: mutable.Seq[Any] = mutable.Seq()
    private var al: Int = 0

    private var pts: Int = 0
    private var pv: mutable.Seq[Any] = mutable.Seq()
    private var pl: Int = 0
    private var proposedValues: mutable.Seq[Any] = mutable.Seq()

    // TODO Determine if Any type needs to change
    private var readList: mutable.HashMap[NetAddress, Any] = mutable.HashMap()

    private var accepted: mutable.HashMap[NetAddress, Int] = mutable.HashMap()
    private var decided: mutable.HashMap[NetAddress, Int] = mutable.HashMap()

    ctrl uponEvent {
        case _: Start => handle {
            // TODO
        }
    }

    def prefix(av: mutable.Seq[Any], al: Int): mutable.Seq[Any] = {
        // TODO
        av
    }
    def suffix(av: mutable.Seq[Any], l: Int): mutable.Seq[Any]  = {
        // TODO
        av
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
                proposedValues = mutable.Seq(v)
                readList = mutable.HashMap()
                accepted = mutable.HashMap()
                decided = mutable.HashMap()
                for (p <- topology) {
                    trigger(NetMessage(self, p, PrepareMessage(pts, al, t)) -> fpl)
                }
            } else if (readList.size <= (N/2).floor) {
                proposedValues = proposedValues :+ v
            } else if (!pv.contains(v)) {
                pv = pv :+ v
                for (p <- topology) {
                    trigger(NetMessage(self, p, AcceptMessage(pts, Seq(v), pv.size - 1, t)) -> fpl)
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
                trigger(NetMessage(self, source, PrepareAckMessage(ts, ats, suffix(av, l), al, t)) -> fpl)
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
                if (readList.size == ((N/2).floor + 1)) {
                    var (tsprime: Int, vsuffixPrime: mutable.Seq[Any]) = (0, mutable.Seq[Any]())
                    // TODO Is there a cleaner way to express the if-statement below?
                    // TODO Right now it's just copy-pasta from the PDF
                    for ((tsPrimePrime: Int, vsuffixPrimePrime: mutable.Seq[Any]) <- readList.values) {
                        if ((tsprime < tsPrimePrime) || (tsprime == tsPrimePrime && vsuffixPrime.size < vsuffixPrimePrime.size)) {
                            tsprime = tsPrimePrime
                            vsuffixPrime = vsuffixPrimePrime
                        }
                    }
                    pv = pv ++ vsuffixPrime
                    for (v <- proposedValues if !pv.contains(v)) {
                        pv = pv :+ v
                    }
                    for (p <- topology if Some(readList(p)).isDefined) {
                        val lPrime = decided(p)
                        trigger(NetMessage(self, p, AcceptMessage(pts, suffix(pv, lPrime), lPrime, t)) -> fpl)
                    }
                } else if (readList.size > ((N/2).floor + 1)) {
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
                val acceptors = for (p <- topology if accepted(p) >= l) yield p
                if ((pl < l) && (acceptors.size > (N/2).floor)) {
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
}






