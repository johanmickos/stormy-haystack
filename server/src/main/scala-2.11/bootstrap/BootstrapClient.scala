package bootstrap

import java.util.UUID

import bootstrap.ClientState.State
import com.typesafe.scalalogging.StrictLogging
import ex.{TAddress, TMessage}
import se.sics.kompics.sl._
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, NegativePort}
import se.sics.kompics.timer.{CancelPeriodicTimeout, SchedulePeriodicTimeout, Timer}

object ClientState extends Enumeration {
    type State = Value
    val Waiting, Started = Value
}

class BootstrapClient extends ComponentDefinition with StrictLogging {
    val bootstrap: NegativePort[Bootstrapping] = provides[Bootstrapping]

    val timer = requires[Timer]
    val network = requires[Network]

    val self: TAddress = cfg.getValue[TAddress]("stormy.address")
    val server: TAddress = cfg.getValue[TAddress]("stormy.bootstrapAddress")

    private var state: State = ClientState.Waiting
    private var timeoutId: Option[UUID] = None

    ctrl uponEvent {
        case _: Start => handle {
            logger.info(s"Starting bootstrap client on $self")
            val timeout: Long = cfg.getValue[Long]("stormy.keepAlivePeriod") * 2
            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(new BootstrapTimeout(spt))
            trigger(spt -> timer)
            timeoutId = Some(spt.getTimeoutEvent.getTimeoutId)
        }
    }
    network uponEvent {
        case context@TMessage(source, self,  Boot(assignment)) => handle {
            logger.info(s"Booting up $self")
            logger.debug(s"$context with $assignment")
            trigger(Booted(assignment) -> bootstrap)
            trigger(new CancelPeriodicTimeout(timeoutId.get) -> timer)
            trigger(TMessage(self, server, Ready) -> network)
            state = ClientState.Started
        }
    }

    timer uponEvent {
        case _: BootstrapTimeout => handle {
            state match {
                case ClientState.Waiting =>
                    trigger(TMessage(self, server, CheckIn) -> network)
                case ClientState.Started =>
                    trigger(TMessage(self, server, Ready) -> network)
                    suicide() // TODO Determine why we suicide here (and what it does)
            }
        }
    }

    override def tearDown(): Unit = {
        trigger(new CancelPeriodicTimeout(timeoutId.get) -> timer)
    }
}
