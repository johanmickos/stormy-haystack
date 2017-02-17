package overlay

import java.util

import ex.TAddress

import scala.collection.{mutable, _}

class PartitionLookupTable(nodes: collection.immutable.Set[TAddress]) extends NodeAssignment {
    val partitions = new mutable.HashMap[Int, mutable.Set[TAddress]] with mutable.MultiMap[Int, TAddress]
    // TODO test below
    partitions ++ (nodes)

    def getNodes: Iterable[TAddress] = partitions.values.flatten

    def lookup(key: String): Option[mutable.Set[TAddress]] = {
        val keyHash: Int = key.hashCode
        val partition: Int = partitions.keySet.minBy(value => math.abs(value - keyHash))
        partitions.get(partition)
    }
}
