package bootstrap

import java.util.UUID

import bootstrap.ClientState.State
import com.typesafe.scalalogging.StrictLogging
import networking.{NetAddress, NetMessage}
import overlay.PartitionLookupTable
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, NegativePort, _}
import se.sics.kompics.timer.{CancelPeriodicTimeout, SchedulePeriodicTimeout, Timer}

object ClientState extends Enumeration {
    type State = Value
    val Waiting, Started = Value
}

class BootstrapClient extends ComponentDefinition with StrictLogging {
    val bootstrap: NegativePort[Bootstrapping] = provides[Bootstrapping]

    val timer = requires[Timer]
    val network = requires[Network]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    val server: NetAddress = cfg.getValue[NetAddress]("stormy.coordinatorAddress")

    private var state: State = ClientState.Waiting
    private var timeoutId: Option[String] = None

    ctrl uponEvent {
        case _: Start => handle {
            logger.info(s"Starting bootstrap client on $self")
            val timeout: Long = cfg.getValue[Long]("stormy.keepAlivePeriod") * 2
            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(new BootstrapTimeout(spt))
            trigger(spt -> timer)
            timeoutId = Some(spt.getTimeoutEvent.getTimeoutId.toString)
        }
    }
    network uponEvent {
        case context@NetMessage(source, self,  Boot(assignment: PartitionLookupTable)) => handle {
            logger.info(s"Booting up $self")
            logger.debug(s"$context with $assignment")
            trigger(Booted(assignment) -> bootstrap)
            trigger(new CancelPeriodicTimeout(UUID.fromString(timeoutId.get)) -> timer)
            trigger(NetMessage(self, server, Ready) -> network)
            state = ClientState.Started
        }
    }

    timer uponEvent {
        case _: BootstrapTimeout => handle {
            state match {
                case ClientState.Waiting =>
                    trigger(NetMessage(self, server, CheckIn) -> network)
                case ClientState.Started =>
                    trigger(NetMessage(self, server, Ready) -> network)
                    suicide() // TODO Determine why we suicide here (and what it does)
            }
        }
    }

    override def tearDown(): Unit = {
        trigger(new CancelPeriodicTimeout(UUID.fromString(timeoutId.get)) -> timer)
    }
}
