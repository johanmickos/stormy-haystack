import converters.TAddressConverter
import networking.{NetAddress, NetHeader, NetMessage}
import overlay.PartitionLookupTable
import se.sics.kompics.Kompics
import se.sics.kompics.config.Conversions
import se.sics.kompics.network.netty.serialization.Serializers
import serialization.PickleSerializer

object Main {

    Serializers.register(PickleSerializer, "pickleS")
    Serializers.register(classOf[PartitionLookupTable], "pickleS")
    Serializers.register(classOf[NetAddress], "pickleS")
    Serializers.register(classOf[NetHeader], "pickleS")
    Serializers.register(classOf[NetMessage[_]], "pickleS")

    Conversions.register(new  TAddressConverter())

    def main(args: Array[String]): Unit = {
        try {
            Kompics.createAndStart(classOf[HostComponent])
            Kompics.waitForTermination()
        } catch {
            case ex: Throwable =>
                println(s"Unexpected exception: $ex")
                ex.printStackTrace()
        }
    }
}
