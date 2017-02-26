package stormy.kv

import java.util.UUID

import se.sics.kompics.KompicsEvent
import stormy.networking.NetAddress
sealed trait Code
case object NotImplemented extends Code
case object NotFound extends Code
case object CASFailed extends Code
case object Ok extends Code

//  TODO Decide on key type & partitioning
object Operation {
    def genId(): String = {
        UUID.randomUUID().toString
    }
}
abstract class Operation(val key: String, val id: String, val client: NetAddress) extends KompicsEvent {
    override def toString: String = s"Operation(key=$key, id=$id)"
}

case class GetOperation(override val key: String, override val id: String, override val client: NetAddress)
    extends Operation(key, id, client)

case class PutOperation(override val key: String, value: String, override val id: String, override val client: NetAddress)
    extends Operation(key, id, client)

case class CASOperation(override val key: String, refValue: String, newValue: String, override val id: String, override val client: NetAddress)
    extends Operation(key, id, client)


case class OperationResponse(id: String, content: Option[String], status: Code, operation: Operation) extends KompicsEvent

