package bootstrap

import ex.TAddress
import overlay.NodeAssignment
import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl._

class Bootstrapping extends Port {
    indication[GetInitialAssignments]
    indication[Booted]
    request[InitialAssignments]
}

case class GetInitialAssignments(nodes: collection.immutable.Set[TAddress]) extends KompicsEvent
case class Booted(assignment: NodeAssignment) extends KompicsEvent
case class InitialAssignments(assignment: NodeAssignment) extends KompicsEvent

final case class Boot(assignment: NodeAssignment) extends KompicsEvent

object CheckIn extends KompicsEvent
object Ready extends KompicsEvent
