package stormy.overlay

import stormy.networking.NetAddress

import scala.collection.{mutable, _}


class PartitionLookupTable(val replicationFactor: Int) {

    var partitions: mutable.MultiMap[Int, NetAddress] = new mutable.HashMap[Int, mutable.Set[NetAddress]] with mutable.MultiMap[Int, NetAddress]
    var ranks: mutable.HashMap[NetAddress, Int] = mutable.HashMap()

    def generate(nodes: collection.immutable.Set[NetAddress]): Unit = {
        val numPartitions: Int = (nodes.size / replicationFactor).floor.toInt
        for ((node, i) <- nodes.zipWithIndex) {
            val partition: Int = i % numPartitions
            partitions.addBinding(partition, node)
            val rank = i % replicationFactor + 1
            ranks(node) = rank
        }
    }

    def isUnderReplicated: Boolean = {
        // Under-replicated? Should probably be caught in whomever is managing us
        // to trigger Stop Signal msg or lock the under-replicated partition
        partitions.values.flatten.size < partitions.size * replicationFactor
    }

    def getNodes: Iterable[NetAddress] = {
        partitions.values.flatten
    }

    def lookup(key: String): mutable.Set[NetAddress] = {
        val keyHash: Int = key.hashCode
        val partition: Int = Some(partitions.keySet.minBy(it => math.abs(keyHash - it))).getOrElse(partitions.keySet.last)
        partitions(partition)
    }

    override def toString = {
        val sb: StringBuilder = new StringBuilder()
        sb.append("PartitionLookupTable (\n")
        partitions.keySet.toIterator.foreach { key =>
            sb.append(key)
            sb.append(" -> ")
            for (node <- partitions(key)) {
                sb.append(node.getIp.toString).append(":").append(node.getPort).append("[").append(ranks(node)).append("]")
            }
            sb.append("\n")
        }
        sb.append(")")
        sb.toString()
    }
}
