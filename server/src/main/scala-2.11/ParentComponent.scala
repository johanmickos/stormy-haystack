import bootstrap.{BootstrapClient, BootstrapServer, Bootstrapping}
import com.typesafe.scalalogging.StrictLogging
import ex._
import kv.KVService
import overlay.{Routing, VSOverlayManager}
import se.sics.kompics.Component
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.Timer

class ParentComponent extends ComponentDefinition with StrictLogging {
    // Ports
    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]

    // Children
    val kv = create(classOf[KVService], Init.NONE)
    val overlay = create(classOf[VSOverlayManager], Init.NONE)

    val boot: Component = {
        cfg.getValue[String]("stormy.type") match {
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

}
