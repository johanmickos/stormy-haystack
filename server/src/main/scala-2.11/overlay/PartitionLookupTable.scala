package overlay

import java.util

import com.google.common.collect.{ImmutableSet, TreeMultimap}
import ex.TAddress

class PartitionLookupTable(nodes: ImmutableSet[TAddress]) extends NodeAssignment {
    private val partitions = TreeMultimap.create[Int, TAddress]()
    partitions.putAll(0, nodes)

    def getNodes: util.Collection[TAddress] = partitions.values()

    def lookup(key: String): util.Collection[TAddress] = {
        val keyHash: Int = key.hashCode
        val partition: Int = Some(partitions.keySet().floor(keyHash)).getOrElse(partitions.keySet().last())
        partitions.get(partition)
    }
}
