package networking

import se.sics.kompics.KompicsEvent
import se.sics.kompics.network.{Msg, Transport}

final case class TMessage[C <: KompicsEvent](src: TAddress, dst:TAddress, payload: C) extends Msg[TAddress, THeader] with Serializable {
    val serialVersionUID:Long = -5669973156467202337L

    def header: THeader = THeader(src, dst, Transport.TCP)

    override def getDestination: TAddress = header.dst

    override def getHeader: THeader = header

    override def getProtocol: Transport = header.proto

    override def getSource: TAddress = header.src

    override def toString: String = super.toString

}
