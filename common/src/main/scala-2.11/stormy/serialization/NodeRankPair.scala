package stormy.serialization

import stormy.networking.NetAddress
import stormy.overlay.PartitionLookupTable
import stormy.serialization.LutPickler.{intPickler, mapPickler, mutableMapPickler}

import scala.pickling._

class NodeRankPair(val node: NetAddress, val rank: Int)


object NodeRankPairPickler extends Pickler[NodeRankPair]
    with Unpickler[NodeRankPair] with pickler.AllPicklers {

    import scala.pickling.shareNothing._

    override def pickle(picklee: NodeRankPair, builder: PBuilder): Unit = {
        builder.hintTag(tag) // This is always required
        builder.beginEntry(picklee)
        builder.putField("node", { fieldBuilder =>
            fieldBuilder.hintTag(NetAddressPickler.tag)
            fieldBuilder.hintStaticallyElidedType()
            NetAddressPickler.pickle(picklee.node, fieldBuilder)
        })
        builder.putField("rank", { fieldBuilder =>
            fieldBuilder.hintTag(intPickler.tag)
            fieldBuilder.hintStaticallyElidedType()
            intPickler.pickle(picklee.rank, fieldBuilder)
        })
        builder.endEntry()
    }

    override def unpickle(tag: String, reader: PReader): Any = {
        val nodeReader = reader.readField("node")
        nodeReader.hintStaticallyElidedType()
        val node: NetAddress = NetAddressPickler.unpickleEntry(nodeReader).asInstanceOf[NetAddress]

        val rankReader = reader.readField("rank")
        rankReader.hintStaticallyElidedType()
        val rank: Int = intPickler.unpickleEntry(rankReader).asInstanceOf[Int]

        new NodeRankPair(node, rank)
    }

    override val tag: FastTypeTag[NodeRankPair] = FastTypeTag[NodeRankPair]

}
