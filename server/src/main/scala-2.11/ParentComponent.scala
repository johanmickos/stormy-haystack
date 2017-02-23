import bootstrap.{BootstrapClient, BootstrapServer, Bootstrapping}
import com.typesafe.scalalogging.StrictLogging
import components.epfd.EPFD
import kv.KVService
import networking.NetAddress
import overlay.{OverlayManager, Routing}
import se.sics.kompics.Component
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.Timer

class ParentComponent extends ComponentDefinition with StrictLogging {
    // Ports
    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]

    // Us
    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")


    // Children
    val kv = create(classOf[KVService], Init.NONE)
    val overlay = create(classOf[OverlayManager], Init.NONE)
    val epfd = create(classOf[EPFD], Init.NONE)
    val bootType: String = cfg.getValue[String]("stormy.type")

    val boot: Component = {
        // TODO Make these static somewhere
        bootType match {
            case "coordinator" =>
                create(classOf[BootstrapServer], Init.NONE)
            case "server" =>
                create(classOf[BootstrapClient], Init.NONE)
        }
    }

    connect[Network](network -> boot)
    connect[Timer](timer -> boot)

    connect[Bootstrapping](boot -> overlay)
    connect[Network](network -> overlay)

    connect[Routing](overlay -> kv)
    connect[Network](network -> kv)

    connect[Routing](overlay -> epfd)
    connect[Network](network -> epfd)
    connect[Timer](timer -> epfd)

}
