package stormy.components.tob

import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port
import stormy.networking.NetAddress

object TOBSpec {
    case class TOB_Deliver(src: NetAddress, payload: KompicsEvent) extends KompicsEvent

    case class TOB_Broadcast(payload: KompicsEvent) extends KompicsEvent

    class TotalOrderBroadcast extends Port {
        request[TOB_Broadcast]
        indication[TOB_Deliver]
    }
}
