package stormy.kv

import java.util.UUID

import se.sics.kompics.KompicsEvent
import stormy.networking.NetAddress

sealed trait Code
case object NotImplemented extends Code
case object NotFound extends Code
case object CASFailed extends Code
case object Ok extends Code


trait Operation extends KompicsEvent {
    val id: String
    val key: String
    val client: NetAddress
    final def genId(): String = {
        UUID.randomUUID().toString
    }
}
final case class GetOperation(key: String, id: String, client: NetAddress) extends Operation
final case class PutOperation(key: String, value: String, id: String, client: NetAddress) extends Operation
final case class CASOperation(key: String, refValue: String, newValue: String, id: String, client: NetAddress) extends Operation

case class OperationResponse(id: String, content: Option[String], status: Code, operation: Operation) extends KompicsEvent

