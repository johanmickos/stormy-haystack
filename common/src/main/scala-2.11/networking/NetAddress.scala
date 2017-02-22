package networking

import java.net.{InetAddress, InetSocketAddress}

import com.google.common.collect.ComparisonChain
import com.google.common.primitives.UnsignedBytes
import se.sics.kompics.network.Address

final case class NetAddress(isa: InetSocketAddress) extends Address with Comparable[NetAddress] {
    override def asSocket(): InetSocketAddress = isa

    override def getIp: InetAddress = isa.getAddress

    override def getPort: Int = isa.getPort

    override def sameHostAs(other: Address): Boolean = {
        this.isa.equals(other.asSocket())
    }

    override def compareTo(that: NetAddress): Int = {
        ComparisonChain.start()
            .compare(this.isa.getAddress.getAddress, that.isa.getAddress.getAddress, UnsignedBytes.lexicographicalComparator())
            .compare(this.isa.getPort, that.isa.getPort)
            .result()
    }

    override def toString: String = isa.toString
}
