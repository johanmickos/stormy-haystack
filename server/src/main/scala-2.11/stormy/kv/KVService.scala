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
        case msg@WrappedOperation(from, op: Operation) => handle {
            logger.debug(s"$self Received routed operation $op")
            trigger(TOB_Broadcast(msg) -> tob)
        }
    }

    tob uponEvent {
        case TOB_Deliver(src, WrappedOperation(coordinator, op: GetOperation)) => handle {
            val value: Option[String] = store.get(op.key)
            trigger(NetMessage(self, coordinator, OperationResponse(op.id, value, getStatus(value), op)) -> network)
        }
        case TOB_Deliver(src, WrappedOperation(coordinator, op: PutOperation)) => handle {
            store(op.key) = op.value
            trigger(NetMessage(self, coordinator, OperationResponse(op.id, Some(op.value), Ok, op)) -> network)
        }
        case ctx@TOB_Deliver(src, WrappedOperation(coordinator, op: CASOperation)) => handle {
            val key = op.key
            val refValue = op.refValue
            val newValue = op.newValue
            if (store.contains(key)) {
                if (store(key).equals(refValue)) {
                    store(key) = newValue
                    trigger(NetMessage(self, coordinator, OperationResponse(op.id, Some(newValue), Ok, op)) -> network)
                } else {
                    trigger(NetMessage(self, coordinator, OperationResponse(op.id, Some(refValue), CASFailed, op)) -> network)
                }
            } else {
                trigger(NetMessage(self, coordinator, OperationResponse(op.id, None, NotFound, op)) -> network)
            }
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
