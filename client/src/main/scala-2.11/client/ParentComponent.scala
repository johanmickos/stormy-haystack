package client

import kvstore.ClientService
import networking.NetAddress
import se.sics.kompics.network.Network
import se.sics.kompics.{Channel, Component, ComponentDefinition, Init}
import se.sics.kompics.network.netty.{NettyInit, NettyNetwork}
import se.sics.kompics.timer.Timer
import se.sics.kompics.timer.java.JavaTimer

class ParentComponent extends ComponentDefinition {
    val self: NetAddress = config.getValue("stormy.address", classOf[NetAddress])
    val timer: Component = create(classOf[JavaTimer], Init.NONE)
    val network: Component = create(classOf[NettyNetwork], new NettyInit(self))
    val client: Component = create(classOf[ClientService], Init.NONE)

    // TODO Remove java-like syntax
    connect(timer.getPositive(classOf[Timer]), client.getNegative(classOf[Timer]), Channel.TWO_WAY)
    connect(network.getPositive(classOf[Network]), client.getNegative(classOf[Network]), Channel.TWO_WAY)
}
