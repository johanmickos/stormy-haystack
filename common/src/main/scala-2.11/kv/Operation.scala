package kv

import java.util.UUID

import kv.Code.Code
import se.sics.kompics.KompicsEvent

object Code extends Enumeration {
    type Code = Value
    val NotImplemented, Ok, NotFound = Value
}
//  TODO Decide on key type & partitioning
case class Operation(key: String) extends KompicsEvent {
    val id: String = UUID.randomUUID().toString
}
case class OperationResponse(id: String, status: Code) extends KompicsEvent

