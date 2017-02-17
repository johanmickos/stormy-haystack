package kv

import com.typesafe.scalalogging.StrictLogging
import ex.{TAddress, TMessage}
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort, handle}
import se.sics.kompics.timer.Timer

class KVService extends ComponentDefinition with StrictLogging {
    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]

    val self: TAddress = cfg.getValue[TAddress]("stormy.address")

    network uponEvent {
        case context@TMessage(source, self, op: Operation) => handle {
            logger.info(s"Got operation $op! Now implement me please :)")
            trigger(TMessage(self, source, OperationResponse(op.id, Code.NotImplemented)) -> network)
        }
    }

}
