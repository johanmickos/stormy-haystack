import converters.TAddressConverter
import ex.{TAddress, THeader, TMessage}
import se.sics.kompics.Kompics
import se.sics.kompics.config.Conversions
import se.sics.kompics.network.netty.serialization.Serializers
import serialization.PickleSerializer

object Main {
    Serializers.register(PickleSerializer, "pickleS")
    Serializers.register(classOf[TAddress], "pickleS")
    Serializers.register(classOf[THeader], "pickleS")
    Serializers.register(classOf[TMessage[_]], "pickleS")

    Conversions.register(TAddressConverter)

    def main(args: Array[String]): Unit = {
        Kompics.createAndStart(classOf[HostComponent])
    }
}
