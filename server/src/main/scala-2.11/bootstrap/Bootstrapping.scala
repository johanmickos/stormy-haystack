package bootstrap

import networking.NetAddress
import overlay.PartitionLookupTable
import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl._

class Bootstrapping extends Port {
    indication[GetInitialAssignments]
    indication[Booted]
    request[InitialAssignments]
}

case class GetInitialAssignments(nodes: collection.immutable.Set[NetAddress]) extends KompicsEvent
case class Booted(assignment: PartitionLookupTable) extends KompicsEvent
case class InitialAssignments(assignment: PartitionLookupTable) extends KompicsEvent

final case class Boot(assignment: PartitionLookupTable) extends KompicsEvent

object CheckIn extends KompicsEvent
object Ready extends KompicsEvent
