package overlay

import java.util.UUID

import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port

class Routing extends Port {
    request[RouteMessage]
}

case class RouteMessage(key: String, msg: KompicsEvent) extends KompicsEvent
case class Connect(id: UUID) extends KompicsEvent
case class Ack(id: UUID, clusterSize: Int) extends KompicsEvent