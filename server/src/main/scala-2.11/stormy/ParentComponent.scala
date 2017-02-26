package stormy

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Component
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.Timer
import stormy.bootstrap.{BootstrapClient, BootstrapServer, Bootstrapping}
import stormy.components.Ports.AbortableConsensus
import stormy.components.consensus.ASC
import stormy.components.eld.ELDSpec.EventualLeaderDetector
import stormy.components.eld.Omega
import stormy.components.epfd.EPDFSpec.EventuallyPerfectFailureDetector
import stormy.components.epfd.EPFD
import stormy.components.tob.TOB
import stormy.kv.KVService
import stormy.networking.NetAddress
import stormy.overlay.{OverlayManager, Routing}

class ParentComponent extends ComponentDefinition with StrictLogging {

    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    val bootType: String = cfg.getValue[String]("stormy.type")

    // Components
    val kv = create(classOf[KVService], Init.NONE)
    val overlay = create(classOf[OverlayManager], Init.NONE)
    val epfd = create(classOf[EPFD], Init.NONE)
    val omega = create(classOf[Omega], Init.NONE)
    val asc = create(classOf[ASC], Init.NONE)
    val tob = create(classOf[TOB], Init.NONE)

    val boot: Component = {
        // TODO Make these static somewhere
        bootType match {
            case "coordinator" =>
                create(classOf[BootstrapServer], Init.NONE)
            case "server" =>
                create(classOf[BootstrapClient], Init.NONE)
        }
    }

    // Bootstrap
    connect[Network](network -> boot)
    connect[Timer](timer -> boot)

    // Overlay
    connect[Bootstrapping](boot -> overlay)
    connect[Network](network -> overlay)

    // KV Store
    connect[Routing](overlay -> kv)
    connect[Network](network -> kv)

    // Failure detector
    connect[Routing](overlay -> epfd)
    connect[Network](network -> epfd)
    connect[Timer](timer -> epfd)

    // Leader elector
    connect[Routing](overlay -> omega)
    connect[EventuallyPerfectFailureDetector](epfd -> omega)

    // Abortable consensus
    connect[Network](network -> asc)

    // Total order broadcast
    connect[Timer](timer -> tob)
    connect[Network](network -> tob)
    connect[EventuallyPerfectFailureDetector](epfd -> tob)
    connect[EventualLeaderDetector](omega -> tob)
    connect[AbortableConsensus](asc -> tob)

}
