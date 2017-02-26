package stormy.serialization

import stormy.networking.NetAddress
import stormy.overlay.PartitionLookupTable

import scala.collection.mutable
import scala.pickling._
import scala.pickling.json._

import scala.pickling.shareNothing._

object LutPickler extends Pickler[PartitionLookupTable]
    with Unpickler[PartitionLookupTable]
    with pickler.AllPicklers
{

    override val tag = FastTypeTag[PartitionLookupTable]

    override def pickle(picklee: PartitionLookupTable, builder: PBuilder): Unit = {
        builder.hintTag(tag) // This is always required
        builder.beginEntry(picklee)
        builder.putField("replicationFactor", { fieldBuilder =>
            fieldBuilder.hintTag(intPickler.tag)
            fieldBuilder.hintStaticallyElidedType()
            intPickler.pickle(picklee.replicationFactor, fieldBuilder)
        })
        builder.putField("partitions", { fieldBuilder =>
            fieldBuilder.hintTag(mapPickler[Int, Set[NetAddress]].tag)
            fieldBuilder.hintStaticallyElidedType()
            mapPickler[Int, Set[NetAddress]].pickle(picklee.partitions.map(kv => (kv._1, kv._2.toSet)).toMap, fieldBuilder)
        })
        builder.putField("numNodes", { fieldBuilder =>
            fieldBuilder.hintTag(intPickler.tag)
            fieldBuilder.hintStaticallyElidedType()
            intPickler.pickle(picklee.getNodes.size, fieldBuilder)
        })

        builder.putField("nprs", { fieldBuilder =>
            var coll: Array[NodeRankPair] = Array()
            for ((node, rank) <- picklee.ranks) {
                val pair = new NodeRankPair(node, rank)
                coll = coll :+ pair
            }
            fieldBuilder.hintStaticallyElidedType()
            fieldBuilder.hintTag(FastTypeTag[Array[NodeRankPair]])
            fieldBuilder.beginEntry(coll)
            fieldBuilder.beginCollection(coll.length)

            for (el <- coll) {
                fieldBuilder.hintTag(NodeRankPairPickler.tag)
                fieldBuilder.putElement( pb => {
                    NodeRankPairPickler.pickle(el, pb)
                })
            }
            fieldBuilder.endCollection()
            fieldBuilder.endEntry()


        })
        builder.endEntry()
    }

    override def unpickle(tag: String, reader: PReader): Any = {
        val replicationFactorReader = reader.readField("replicationFactor")
        replicationFactorReader.hintStaticallyElidedType()
        val replicationFactor: Int = intPickler.unpickleEntry(replicationFactorReader).asInstanceOf[Int]

        val partitionReader = reader.readField("partitions")
        partitionReader.hintStaticallyElidedType()
        val entry = mapPickler[Int, Set[NetAddress]].unpickleEntry(partitionReader)
        val partitionsMap = entry.asInstanceOf[Map[Int, Set[NetAddress]]]
        val partitions: mutable.MultiMap[Int, NetAddress] = new mutable.HashMap[Int, mutable.Set[NetAddress]] with mutable.MultiMap[Int, NetAddress]

        val numNodesReader = reader.readField("numNodes")
        numNodesReader.hintStaticallyElidedType()
        val numNodes: Int = intPickler.unpickleEntry(numNodesReader).asInstanceOf[Int]

        var ranks: mutable.HashMap[NetAddress, Int] = mutable.HashMap()

        val nrpReader = reader.readField("nprs")
        nrpReader.hintStaticallyElidedType()
        nrpReader.hintTag(FastTypeTag[Array[NodeRankPair]])
        nrpReader.beginEntry()
        nrpReader.hintStaticallyElidedType()
        nrpReader.hintTag(FastTypeTag[Array[NodeRankPair]])
        val collReader = nrpReader.beginCollection()
        val length = collReader.readLength()
        for (n <- 0 until length) {
            val el = collReader.readElement()
            val nrp: NodeRankPair = NodeRankPairPickler.unpickleEntry(el).asInstanceOf[NodeRankPair]
            ranks(nrp.node) = nrp.rank
        }
        nrpReader.endCollection()
        nrpReader.endEntry()


        val lut: PartitionLookupTable = new PartitionLookupTable(replicationFactor)
        for ((partition: Int, nodes: Set[NetAddress]) <- partitionsMap) {
            partitions.put(partition, mutable.Set(nodes.toSeq: _*))
        }
        lut.partitions = partitions
        lut.ranks = ranks
        lut
    }
}
