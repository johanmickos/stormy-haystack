package stormy.kv

import java.util.UUID

import se.sics.kompics.KompicsEvent
sealed trait Code
case object NotImplemented extends Code
case object NotFound extends Code
case object Ok extends Code

//  TODO Decide on key type & partitioning
object Operation {
    def genId(): String = {
        UUID.randomUUID().toString
    }
}
final case class Operation(key: String, id: String) extends KompicsEvent {
    override def toString: String = s"Operation(key=$key, id=$id)"
}
case class OperationResponse(id: String, status: Code) extends KompicsEvent

