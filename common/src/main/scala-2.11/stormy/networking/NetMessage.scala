package stormy.networking

import se.sics.kompics.KompicsEvent
import se.sics.kompics.network.{Msg, Transport}

final case class NetMessage[C <: KompicsEvent](src: NetAddress, dst:NetAddress, payload: C) extends Msg[NetAddress, NetHeader] with Serializable {
    val serialVersionUID:Long = -5669973156467202337L

    def header: NetHeader = NetHeader(src, dst, Transport.TCP)

    override def getDestination: NetAddress = header.dst

    override def getHeader: NetHeader = header

    override def getProtocol: Transport = header.proto

    override def getSource: NetAddress = header.src

    override def toString: String = super.toString

}
