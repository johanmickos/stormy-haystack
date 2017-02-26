package stormy.kv

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort, handle}
import stormy.components.tob.TOBSpec.{TOB_Broadcast, TOB_Deliver, TotalOrderBroadcast}
import stormy.networking.{NetAddress, NetMessage}
import stormy.overlay.Routing

class KVService extends ComponentDefinition with StrictLogging {
    val network: PositivePort[Network] = requires[Network]
    val routing: PositivePort[Routing] = requires[Routing]
    val tob: PositivePort[TotalOrderBroadcast] = requires[TotalOrderBroadcast]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")


    routing uponEvent {
        case op: Operation => handle {
            logger.info("Received routed operation!")
            trigger(TOB_Broadcast(op) -> tob)
        }
    }

    tob uponEvent {
        case TOB_Deliver(src, op: Operation) => handle {
            logger.info(s"$self Got TOB_Deliver $op")
            trigger(NetMessage(self, src, OperationResponse(op.id, NotImplemented, op)) -> network)
        }
    }

}
