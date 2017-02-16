package kv

import java.util.UUID

import kv.Code.Code
import se.sics.kompics.KompicsEvent

object Code extends Enumeration {
    type Code = Value
    val NotImplemented, Ok, NotFound = Value
}
//  TODO Decide on key type & partitioning
case class Operation(key: String, id: UUID) extends KompicsEvent
case class OperationResponse(id: UUID, code: Code) extends KompicsEvent

