package client

import java.net.InetSocketAddress
import java.util.UUID

import org.apache.commons.cli.{CommandLineParser, DefaultParser, Options}
import se.sics.kompics.Kompics
import se.sics.kompics.config.{Config, Conversions, ValueMerger}
import se.sics.kompics.network.netty.serialization.Serializers
import stormy.converters.TAddressConverter
import stormy.networking.{NetAddress, NetHeader, NetMessage}
import stormy.serialization.PickleSerializer

object Client {

    val converter = new TAddressConverter

    Conversions.register(converter)
    Serializers.register(PickleSerializer, "pickleS")
    Serializers.register(classOf[NetAddress], "pickleS")
    Serializers.register(classOf[NetHeader], "pickleS")
    Serializers.register(classOf[NetMessage[_]], "pickleS")

    def main(args: Array[String]): Unit = {
        val opts: Options = prepareOptions
        val clientParser: CommandLineParser = new DefaultParser
        val cmd = clientParser.parse(opts, args)

        val cfg = Kompics.getConfig.asInstanceOf[Config.Impl]
        var self: NetAddress = cfg.getValue("stormy.address", classOf[NetAddress])
        val cb: Config.Builder = cfg.modify(UUID.randomUUID())
        if (cmd.hasOption("p") || cmd.hasOption("i")) {
            var ip: String = self.asSocket().getHostString
            var port: Int = self.getPort
            if (cmd.hasOption("p")) {
                port = cmd.getOptionValue("p").toInt
            }
            if (cmd.hasOption("i")) {
                ip = cmd.getOptionValue("i")
            }
            self = NetAddress(new InetSocketAddress(ip, port))
        }
        cb.setValue("stormy.address", self)
        if (cmd.hasOption("b")) {
            val serverS = cmd.getOptionValue("b")
            val server = converter.convert(serverS)
            if (server == null) {
                System.err.println("Couldn't parse address string: " + serverS)
                System.exit(1)
            }
            cb.setValue("stormy.coordinatorAddress", server)
        }
        val cu = cb.finalise
        cfg.apply(cu, ValueMerger.NONE)
        Kompics.createAndStart(classOf[ParentComponent])
        Kompics.waitForTermination()
    }

    private def prepareOptions: Options = {
        val opts = new Options
        opts.addOption("b", true, "Set Bootstrap server to <arg> (ip:port)")
        opts.addOption("p", true, "Change local port to <arg> (default from config file)")
        opts.addOption("i", true, "Change local ip to <arg> (default from config file)")
        opts
    }
}
