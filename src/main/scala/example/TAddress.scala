package example

import java.net.{InetAddress, InetSocketAddress}

import se.sics.kompics.network.Address

final case class TAddress(isa: InetSocketAddress) extends Address {
  override def getIp: InetAddress = isa.getAddress

  override def sameHostAs(other: Address): Boolean = {
    this.isa.eq(other.asSocket())
  }

  override def getPort: Int = isa.getPort

  override def asSocket(): InetSocketAddress = isa
}
