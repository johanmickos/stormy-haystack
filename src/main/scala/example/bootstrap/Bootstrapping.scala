package example.bootstrap

import example.bootstrap.networking.NetAddress
import se.sics.kompics.{KompicsEvent, PortType}
import se.sics.kompics.sl.Port

// TODO Fix all parameters
case class GetInitialAssignments(nodes: collection.immutable.Set[NetAddress]) extends KompicsEvent
case class Booted(assignment: NodeAssignment) extends KompicsEvent
case class InitialAssignments(nodes: collection.immutable.Set[NetAddress]) extends KompicsEvent

class Bootstrapping extends Port {
    indication[GetInitialAssignments]
    indication[Booted]
    request[InitialAssignments]
}
