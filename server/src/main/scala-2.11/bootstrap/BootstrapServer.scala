package bootstrap

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.sl.ComponentDefinition

class BootstrapServer extends ComponentDefinition with StrictLogging {
    provides[Bootstrapping]

}
