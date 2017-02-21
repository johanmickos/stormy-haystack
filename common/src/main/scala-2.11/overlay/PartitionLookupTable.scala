package overlay

import ex.TAddress

import scala.collection.{mutable, _}

class PartitionLookupTable  {
    var partitions: mutable.MultiMap[Int, TAddress] = new mutable.HashMap[Int, mutable.Set[TAddress]] with mutable.MultiMap[Int, TAddress]

    def generate(nodes: collection.immutable.Set[TAddress]): Unit = {
        for ((node, i) <- nodes.zipWithIndex) {
//            partitions.put(i, collection.mutable.Set(node))
            partitions.addBinding(0, node)

        }
    }
    def getNodes: Iterable[TAddress] = {
//        partitions.values.asScala
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
