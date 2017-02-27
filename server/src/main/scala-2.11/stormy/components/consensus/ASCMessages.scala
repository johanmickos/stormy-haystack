package stormy.components.consensus

import se.sics.kompics.KompicsEvent


object ASCMessages {

    case class PrepareMessage(timestamp: Int, length: Int, logicalClock: Int) extends KompicsEvent

    case class AcceptMessage(timestamp: Int, seq: List[Any], length: Int, logicalClock: Int) extends KompicsEvent

    case class NackMessage(timestamp: Int, logicalClock: Int) extends KompicsEvent

    case class PrepareAckMessage(timestamp: Int, acceptorTimestamp: Int, suffix: List[Any], length: Int, logicalClock: Int) extends KompicsEvent

    case class AcceptAckMessage(timestamp: Int, length: Int, logicalClock: Int) extends KompicsEvent

    case class DecideMessage(timestamp: Int, length: Int, logicalClock: Int) extends KompicsEvent

}

