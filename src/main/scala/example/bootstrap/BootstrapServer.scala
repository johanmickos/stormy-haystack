package example.bootstrap


import java.net.InetAddress
import java.util.UUID

import com.google.common.collect.ImmutableSet
import example.bootstrap.networking.{Message, NetAddress}
import se.sics.kompics.Start
import se.sics.kompics.network.netty.NettyNetwork
import se.sics.kompics.sl.{ComponentDefinition, _}
import se.sics.kompics.timer.{CancelPeriodicTimeout, SchedulePeriodicTimeout, Timer}

import scala.collection.JavaConverters._
import scala.collection.mutable


object State extends Enumeration {
    type State = Value
    val Collecting, Seeding, Done = Value
}
class BootstrapServer(init: Init[BootstrapServer]) extends ComponentDefinition {
    import example.bootstrap.State.State


    val boot = provides[Bootstrapping]
    var network = requires[NettyNetwork]
    var timer = requires[Timer]

//    val self: String = cfg.getValue("sh.address")
    val self = new NetAddress(InetAddress.getByName("http://localhost"), 8082)

    private var timeoutId: Option[UUID] = None
    private var active:mutable.Set[NetAddress] = collection.mutable.Set[NetAddress]()
    private var ready:mutable.Set[NetAddress] = collection.mutable.Set[NetAddress]()
    private var initialAssignment = None : Option[NodeAssignment]
    private var state:State = State.Collecting


    ctrl uponEvent {
        case _: Start => handle {
            val timeout: Long = 2L // TODO
            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(new BootstrapTimeout(spt))
            trigger(spt -> timer)
            timeoutId = Some(spt.getTimeoutEvent.getTimeoutId)
            active = active + self
        }
    }

    timer uponEvent {
        case BootstrapTimeout(_) => handle {
            import example.bootstrap.State._

            state match {
                case Collecting =>
                    if (active.size >= 3) {
                        // TODO bootThreshold
                        bootUp()
                    }
                case Seeding =>
                    if (ready.size >= 3) {
                        // TODO bootThreshold
                        trigger(Booted(initialAssignment.get) -> boot)
                        state = Done
                    }
                case Done =>
                    suicide()
            }
        }
    }

    network uponEvent  {
        case context  @ Message(_, from, CheckIn) => handle {
        }

    }

    private def bootUp(): Unit = {
        state = State.Seeding
        trigger(GetInitialAssignments(collection.immutable.Set[NetAddress](active.toSeq: _*)) -> boot)
    }

    override def tearDown(): Unit = {
        trigger(new CancelPeriodicTimeout(timeoutId.get) -> timer)
    }

}
