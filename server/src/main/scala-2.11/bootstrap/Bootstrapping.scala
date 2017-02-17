package bootstrap

import com.google.common.collect.ImmutableSet
import ex.TAddress
import overlay.NodeAssignment
import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl._

class Bootstrapping extends Port {
    indication[GetInitialAssignments]
    indication[Booted]
    request[InitialAssignments]
}

case class GetInitialAssignments(nodes: ImmutableSet[TAddress]) extends KompicsEvent
case class Booted(assignment: NodeAssignment) extends KompicsEvent
case class InitialAssignments(assignment: NodeAssignment) extends KompicsEvent