package stormy.components

import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl.Port
import stormy.networking.NetAddress

package object eld {

    case class Trust(process: NetAddress) extends KompicsEvent

    class EventualLeaderDetector extends Port {
        indication[Trust]
    }

}
