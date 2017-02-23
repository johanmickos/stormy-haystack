package stormy.components.epfd

import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timeout}
import stormy.networking.NetAddress

//Custom messages to be used in the internal component implementation
case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout)

case class Suspect(process: NetAddress) extends KompicsEvent

case class Restore(process: NetAddress) extends KompicsEvent

case class HeartbeatReply(seq: Int) extends KompicsEvent

case class HeartbeatRequest(seq: Int) extends KompicsEvent

class EventuallyPerfectFailureDetector extends Port {
    indication[Suspect]
    indication[Restore]
}