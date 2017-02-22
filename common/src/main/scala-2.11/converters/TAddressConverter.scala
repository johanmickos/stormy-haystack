package converters

import java.net.{InetAddress, InetSocketAddress}
import java.util.Map

import se.sics.kompics.config.Conversions
import se.sics.kompics.config.Converter
import com.typesafe.scalalogging.LazyLogging
import networking.TAddress

/**
  * Custom Java-esque converter for parsing application configurations with network addresses
  * into TAddress types.
  *
  * To register:
  *     Conversions.register(TAddressConverter);
  */
class TAddressConverter extends Converter[TAddress] with LazyLogging {

    override def convert(o: Object): TAddress = {
        o match {
            case m: Map[String, Any]@unchecked => {
                try {
                    val hostname = Conversions.convert(m.get("host"), classOf[String])
                    val port = Conversions.convert(m.get("port"), classOf[Integer])
                    val ip = InetAddress.getByName(hostname)
                    return TAddress(new InetSocketAddress(ip, port))
                } catch {
                    case ex: Throwable =>
                        logger.error(s"Could not convert $m to TAddress", ex)
                        return null
                }
            }
            case s: String => {
                try {
                    val ipport = s.split(":")
                    val ip = InetAddress.getByName(ipport(0))
                    val port = Integer.parseInt(ipport(1))
                    return TAddress(new InetSocketAddress(ip, port))
                } catch {
                    case ex: Throwable =>
                        logger.error(s"Could not convert '$s' to TAddress", ex)
                        return null
                }
            }
        }
        logger.warn(s"Could not convert $o to TAddress")
        return null
    }

    override def `type`(): Class[TAddress] = {
        return classOf[TAddress]
    }

}