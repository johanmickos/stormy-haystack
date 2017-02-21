import com.typesafe.scalalogging.StrictLogging
import ex.TAddress
import se.sics.kompics.Component
import se.sics.kompics.network.Network
import se.sics.kompics.network.netty.{NettyInit, NettyNetwork}
import se.sics.kompics.sl._
import se.sics.kompics.timer.Timer
import se.sics.kompics.timer.java.JavaTimer

class HostComponent extends ComponentDefinition with StrictLogging {
    val self = cfg.getValue[TAddress]("stormy.address")

    val timer: Component = create(classOf[JavaTimer], Init.NONE)
    val network: Component = create(classOf[NettyNetwork], new NettyInit(self))
    val parent: Component = create(classOf[ParentComponent], Init.NONE)

    connect[Timer](timer -> parent)
    connect[Network](network -> parent)

    ctrl uponEvent {
        case item => handle {
            logger.warn(s"Received unexpected event: ${item}")
        }
    }

}
