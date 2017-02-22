package serialization

import networking.TAddress
import overlay.PartitionLookupTable

import scala.collection.mutable
import scala.pickling._

object LutPickler extends Pickler[PartitionLookupTable] with Unpickler[PartitionLookupTable] with pickler.AllPicklers {
    import scala.pickling.shareNothing._

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
            fieldBuilder.hintTag(mapPickler[Int, Set[TAddress]].tag)
            fieldBuilder.hintStaticallyElidedType()
            mapPickler[Int, Set[TAddress]].pickle(picklee.partitions.map(kv => (kv._1, kv._2.toSet)).toMap, fieldBuilder)
        })
        builder.endEntry()
    }

    override def unpickle(tag: String, reader: PReader): Any = {
        val replicationFactorReader = reader.readField("replicationFactor")
        replicationFactorReader.hintStaticallyElidedType()
        val replicationFactor: Int = intPickler.unpickleEntry(replicationFactorReader).asInstanceOf[Int]

        val partitionReader = reader.readField("partitions")
        partitionReader.hintStaticallyElidedType()
        val entry = mapPickler[Int, Set[TAddress]].unpickleEntry(partitionReader)
        val partitionsMap = entry.asInstanceOf[Map[Int, Set[TAddress]]]
        val partitions: mutable.MultiMap[Int, TAddress] = new mutable.HashMap[Int, mutable.Set[TAddress]] with mutable.MultiMap[Int, TAddress]

        val lut: PartitionLookupTable = new PartitionLookupTable(replicationFactor)
        for ((partition: Int, nodes: Set[TAddress]) <- partitionsMap) {
            partitions.put(partition, mutable.Set(nodes.toSeq: _*))
        }
        lut.partitions = partitions
        lut
    }
}
