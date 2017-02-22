package overlay

import ex.TAddress

import scala.collection.{mutable, _}


class PartitionLookupTable(val replicationFactor: Int)  {
    var partitions: mutable.MultiMap[Int, TAddress] = new mutable.HashMap[Int, mutable.Set[TAddress]] with mutable.MultiMap[Int, TAddress]

    def generate(nodes: collection.immutable.Set[TAddress]): Unit = {
        val numPartitions = (nodes.size % replicationFactor) + 1
        for ((node, i) <- nodes.zipWithIndex) {
            val partition: Int = (i/numPartitions).floor.toInt
            partitions.addBinding(partition, node)
        }
    }

    def isUnderReplicated: Boolean = {
        // Under-replicated? Should probably be caught in whomever is managing us
        // to trigger Stop Signal msg or lock the under-replicated partition
        partitions.values.flatten.size % replicationFactor != 0
    }
    def getNodes: Iterable[TAddress] = {
        partitions.values.flatten
    }

    def lookup(key: String): mutable.Set[TAddress] = {
        val keyHash: Int = key.hashCode
        val partition: Int = Some(partitions.keySet.minBy(it => math.abs(keyHash - it))).getOrElse(partitions.keySet.last)
        partitions(partition)
    }

    override def toString = {
        val sb: StringBuilder = new StringBuilder()
        sb.append("PartitionLookupTable(\n")
        partitions.keySet.toIterator.foreach { key =>
            sb.append(key)
            sb.append(" -> ")
            sb.append(partitions(key))
            sb.append("\n")
        }
        sb.append(")")
        sb.toString()
    }
}
