package example

import se.sics.kompics.network.{Header, Transport}

final case class THeader(src: TAddress, dst: TAddress, proto: Transport) extends Header[TAddress] {
  override def getSource: TAddress = src
  override def getDestination: TAddress = dst
  override def getProtocol: Transport = proto
}
