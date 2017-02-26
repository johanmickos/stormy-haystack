package stormy.kv

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort, handle}
import stormy.components.tob.TOBSpec.{TOB_Broadcast, TOB_Deliver, TotalOrderBroadcast}
import stormy.networking.{NetAddress, NetMessage}
import stormy.overlay.Routing

import scala.collection.mutable

class KVService extends ComponentDefinition with StrictLogging {
    val network: PositivePort[Network] = requires[Network]
    val routing: PositivePort[Routing] = requires[Routing]
    val tob: PositivePort[TotalOrderBroadcast] = requires[TotalOrderBroadcast]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")


    private val store: mutable.HashMap[String, String] = mutable.HashMap()

    routing uponEvent {
        case op: Operation => handle {
            logger.info(s"$self Received routed operation!")
            trigger(TOB_Broadcast(op) -> tob)
        }
    }

    tob uponEvent {
        case TOB_Deliver(src, op: GetOperation) => handle {
            val value: Option[String] = store.get(op.key)
            trigger(NetMessage(self, src, OperationResponse(op.id, value, getStatus(value), op)) -> network)
        }
        case TOB_Deliver(src, op: PutOperation) => handle {
            store(op.key) = op.value
            trigger(NetMessage(self, src, OperationResponse(op.id, Some(op.value), Ok, op)) -> network)
        }
        case TOB_Deliver(src, op: CASOperation) => handle {
            if (store.contains(op.key)) {
                if (store(op.key).eq(op.refValue)) {
                    store(op.key) = op.newValue
                    trigger(NetMessage(self, src, OperationResponse(op.id, Some(op.newValue), Ok, op)) -> network)
                } else {
                    trigger(NetMessage(self, src, OperationResponse(op.id, Some(op.newValue), CASFailed, op)) -> network)
                }
            } else {
                trigger(NetMessage(self, src, OperationResponse(op.id, None, NotFound, op)) -> network)
            }
        }
        case TOB_Deliver(src, op: Operation) => handle {
            logger.info(s"$self Got TOB_Deliver $op")
            trigger(NetMessage(self, src, OperationResponse(op.id, None, NotImplemented, op)) -> network)
        }
    }

    private def getStatus(value: Option[String]): Code = {
        value match {
            case None =>
                NotFound
            case _ =>
                Ok
        }
    }

}
