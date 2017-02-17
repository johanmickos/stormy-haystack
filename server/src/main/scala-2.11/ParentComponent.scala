import bootstrap.BootstrapServer
import com.typesafe.scalalogging.StrictLogging
import ex._
import kv.KVService
import overlay.VSOverlayManager
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

    //  TODO BootServer or BootClient?
    var child: Component = create(classOf[BootstrapServer], Init.NONE)

    // Below is for running ping-pong example
//    val child: Component = {
//        cfg.getValue[String]("pingpong.type") match {
//            case "ponger" =>  create(classOf[Ponger], Init.NONE)
//            case "pinger" =>
//                val ponger = cfg.getValue[TAddress]("pingpong.pinger.pongeraddr")
//                create(classOf[Pinger], Init[Pinger](ponger))
//            case unknown =>
//                println(s"Received: ${unknown}")
//                create(classOf[Ponger], Init.NONE)
//        }
//    }

    // TODO Connect the rest (kv, overlay, boot...)
    connect[Network](network -> child)
    connect[Timer](timer -> child)

}
