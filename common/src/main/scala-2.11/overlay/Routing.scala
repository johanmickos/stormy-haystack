package overlay


import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port

class Routing extends Port {
    request[RouteMessage]
}

case class RouteMessage(key: String, msg: KompicsEvent) extends KompicsEvent
case class Connect(id: String) extends KompicsEvent
case class Ack(id: String, clusterSize: Int) extends KompicsEvent