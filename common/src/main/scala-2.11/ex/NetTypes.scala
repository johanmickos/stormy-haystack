package ex


import java.net.InetAddress
import java.net.InetSocketAddress
import se.sics.kompics.network.{Address, Header, Msg, Transport}
import se.sics.kompics.KompicsEvent

final case class TAddress(isa: InetSocketAddress) extends Address {
    override def asSocket(): InetSocketAddress = isa

    override def getIp(): InetAddress = isa.getAddress

    override def getPort(): Int = isa.getPort

    override def sameHostAs(other: Address): Boolean = {
        this.isa.equals(other.asSocket())
    }
}

final case class THeader(src: TAddress, dst: TAddress, proto: Transport) extends Header[TAddress] {
    override def getDestination(): TAddress = dst

    override def getProtocol(): Transport = proto

    override def getSource(): TAddress = src
}

final case class TMessage[C <: KompicsEvent](header: THeader, payload: C) extends Msg[TAddress, THeader] {
    override def getDestination(): TAddress = header.dst

    override def getHeader(): THeader = header

    override def getProtocol(): Transport = header.proto

    override def getSource(): TAddress = header.src
}