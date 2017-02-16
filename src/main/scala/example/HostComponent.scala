package example

import java.net.InetAddress

import example.bootstrap.networking.NetAddress
import se.sics.kompics.Init
import se.sics.kompics.network.Network
import se.sics.kompics.network.netty.{NettyInit, NettyNetwork}
import se.sics.kompics.sl.ComponentDefinition
import se.sics.kompics.timer.Timer
import se.sics.kompics.timer.java.JavaTimer

class HostComponent extends ComponentDefinition {
//    val self = cfg.getValue[example.TAddress]("host.address") // TODO Define
    val self = new NetAddress(InetAddress.getByName("http://localhost"), 8082)

    val timer = create(classOf[JavaTimer], se.sics.kompics.Init.NONE)
    val network = create(classOf[NettyNetwork], new NettyInit(self))
    val parent = create(classOf[ParentComponent], Init.NONE)

    connect[Timer](timer -> parent)
    connect[Network](network -> parent)

}
