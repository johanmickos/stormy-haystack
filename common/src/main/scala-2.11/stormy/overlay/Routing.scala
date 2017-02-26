package stormy.overlay

import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port
import stormy.kv.Operation

class Routing extends Port {
    request[RouteMessage]
    indication[OverlayUpdate]
    indication[Operation]
}

case class OverlayUpdate(lut: PartitionLookupTable) extends KompicsEvent

case class RouteMessage(key: String, msg: KompicsEvent) extends KompicsEvent

case class Connect(id: String) extends KompicsEvent

case class Ack(id: String, clusterSize: Int) extends KompicsEvent