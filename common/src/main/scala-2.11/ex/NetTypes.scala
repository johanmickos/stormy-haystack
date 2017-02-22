package ex


import java.net.InetAddress
import java.net.InetSocketAddress

import com.google.common.collect.ComparisonChain
import com.google.common.primitives.UnsignedBytes
import se.sics.kompics.network.{Address, Header, Msg, Transport}
import se.sics.kompics.KompicsEvent

final case class TAddress(isa: InetSocketAddress) extends Address with Comparable[TAddress] {
    override def asSocket(): InetSocketAddress = isa

    override def getIp: InetAddress = isa.getAddress

    override def getPort: Int = isa.getPort

    override def sameHostAs(other: Address): Boolean = {
        this.isa.equals(other.asSocket())
    }

    override def compareTo(that: TAddress): Int = {
        ComparisonChain.start()
            .compare(this.isa.getAddress.getAddress, that.isa.getAddress.getAddress, UnsignedBytes.lexicographicalComparator())
            .compare(this.isa.getPort, that.isa.getPort)
            .result()
    }

    override def toString: String = isa.toString
}

final case class THeader(src: TAddress, dst: TAddress, proto: Transport) extends Header[TAddress] {
    override def getDestination: TAddress = dst

    override def getProtocol: Transport = proto

    override def getSource: TAddress = src
}

final case class TMessage[C <: KompicsEvent](src: TAddress, dst:TAddress, payload: C) extends Msg[TAddress, THeader] with Serializable {
    val serialVersionUID:Long = -5669973156467202337L

    def header: THeader = THeader(src, dst, Transport.TCP)

    override def getDestination: TAddress = header.dst

    override def getHeader: THeader = header

    override def getProtocol: Transport = header.proto

    override def getSource: TAddress = header.src

    override def toString: String = super.toString

}