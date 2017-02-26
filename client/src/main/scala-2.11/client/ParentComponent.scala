package client

import kvstore.ClientService
import se.sics.kompics.network.Network
import se.sics.kompics.network.netty.{NettyInit, NettyNetwork}
import se.sics.kompics.sl._
import se.sics.kompics.timer.Timer
import se.sics.kompics.timer.java.JavaTimer
import stormy.networking.NetAddress

class ParentComponent extends ComponentDefinition {
    val self: NetAddress = config.getValue("stormy.address", classOf[NetAddress])
    val timer = create(classOf[JavaTimer], Init.NONE)
    val network = create(classOf[NettyNetwork], new NettyInit(self))
    val client = create(classOf[ClientService], Init.NONE)

    connect[Timer](timer -> client)
    connect[Network](network -> client)

}
