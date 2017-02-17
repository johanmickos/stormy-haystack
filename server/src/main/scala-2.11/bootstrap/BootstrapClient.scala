package bootstrap

import se.sics.kompics.sl.{ComponentDefinition, NegativePort}

class BootstrapClient extends ComponentDefinition {
    val boot: NegativePort[Bootstrapping] = provides[Bootstrapping]

}
