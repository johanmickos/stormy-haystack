package kv

import com.typesafe.scalalogging.StrictLogging
import networking.{TAddress, TMessage}
import overlay.Routing
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort, handle}

class KVService extends ComponentDefinition with StrictLogging {
    val network: PositivePort[Network] = requires[Network]
    val route: PositivePort[Routing] = requires[Routing]

    val self: TAddress = cfg.getValue[TAddress]("stormy.address")

    network uponEvent {
        case context@TMessage(source, self, op: Operation) => handle {
            logger.info(s"Got operation $op with ID ${op.id}! Now implement me please :)")
            trigger(TMessage(self, source, OperationResponse(op.id, NotImplemented)) -> network)
        }
    }

}
