package example

import example.bootstrap.BootstrapServer
import se.sics.kompics.Init
import se.sics.kompics.network.Network
import se.sics.kompics.sl.ComponentDefinition
import se.sics.kompics.timer.Timer

class ParentComponent extends ComponentDefinition {
    val network = requires[Network]
    val timer = requires[Timer]

    val boot = create(classOf[BootstrapServer], Init.NONE)


    connect[Timer](timer -> boot)
    connect[Network](network -> boot)

//    connect[Bootstrapping](boot -> overlay)
//    connect[Network](network -> overlay)
//
//    connect[Routing](overlay -> kv)
//    connect[Network](network -> kv)

}
