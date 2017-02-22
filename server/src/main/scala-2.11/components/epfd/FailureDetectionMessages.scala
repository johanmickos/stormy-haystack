package components.epfd

import networking.TAddress
import se.sics.kompics.KompicsEvent
import se.sics.kompics.sl._
import se.sics.kompics.timer.{ScheduleTimeout, Timeout}

//Custom messages to be used in the internal component implementation
case class CheckTimeout(timeout: ScheduleTimeout) extends Timeout(timeout)

case class Suspect(process: TAddress) extends KompicsEvent
case class Restore(process: TAddress) extends KompicsEvent

case class HeartbeatReply(seq: Int) extends KompicsEvent
case class HeartbeatRequest(seq: Int) extends KompicsEvent

class EventuallyPerfectFailureDetector extends Port {
    indication[Suspect]
    indication[Restore]
}