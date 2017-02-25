package stormy.components.eld

import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port
import stormy.networking.NetAddress

object ELDSpec {
    case class Trust(process: NetAddress) extends KompicsEvent

    class EventualLeaderDetector extends Port {
        indication[Trust]
    }
}
