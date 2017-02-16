package example

import se.sics.kompics.KompicsEvent
import se.sics.kompics.network.{Msg, Transport}

final case class TMessage[C <: KompicsEvent](header: THeader, payload: C) extends Msg[TAddress, THeader] {
  override def getDestination: TAddress = header.dst
  override def getProtocol: Transport = header.proto
  override def getSource: TAddress = header.src
  override def getHeader: THeader = header
}
