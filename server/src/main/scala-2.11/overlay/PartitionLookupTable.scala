package overlay

import java.util
import java.util.Comparator

import com.google.common.collect.ImmutableSet.Builder
import com.google.common.collect.{ImmutableSet, Iterables, TreeMultimap}
import ex.TAddress
import se.sics.kompics.network.Address

import scala.collection.JavaConverters._
import scala.collection.{mutable, _}

class PartitionLookupTable(nodes: collection.immutable.Set[TAddress]) extends NodeAssignment {
//    val partitions: mutable.MultiMap[Int, TAddress] = new mutable.HashMap[Int, mutable.Set[TAddress]] with mutable.MultiMap[Int, TAddress]
    val partitions: TreeMultimap[Integer, TAddress] = TreeMultimap.create[Integer, TAddress]()
    val builder: Builder[TAddress] = ImmutableSet.builder[TAddress]()
    builder.addAll(nodes.asJava)
    partitions.putAll(0,  builder.build())

    def getNodes: Iterable[TAddress] = {
        partitions.values.asScala
    }

    def lookup(key: String): mutable.Set[TAddress] = {
        val keyHash: Int = key.hashCode
        val partition: Int = Some(partitions.keySet().floor(keyHash).intValue()).getOrElse(partitions.keySet().last().intValue())
        partitions.get(partition).asScala
    }

    override def toString = {
        val sb: StringBuilder = new StringBuilder()
        sb.append("PartitionLookupTable(\n")
        partitions.keySet().asScala.toIterator.foreach { key =>
            sb.append(key)
            sb.append(" -> ")
            sb.append(Iterables.toString(partitions.get(key)))
            sb.append("\n")
        }
        sb.append(")")
        sb.toString()
    }
}
