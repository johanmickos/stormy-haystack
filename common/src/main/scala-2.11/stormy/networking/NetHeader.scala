package stormy.networking

import se.sics.kompics.network.{Header, Transport}

final case class NetHeader(src: NetAddress, dst: NetAddress, proto: Transport) extends Header[NetAddress] {
    override def getDestination: NetAddress = dst

    override def getProtocol: Transport = proto

    override def getSource: NetAddress = src
}
