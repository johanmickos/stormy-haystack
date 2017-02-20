package bootstrap

import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import ex.{TAddress, TMessage}
import overlay.NodeAssignment
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, NegativePort, PositivePort, _}
import se.sics.kompics.timer.{CancelPeriodicTimeout, SchedulePeriodicTimeout, Timeout, Timer}

object State extends Enumeration {
    type State = Value
    val Collecting, Seeding, Done = Value
}

class BootstrapServer extends ComponentDefinition with StrictLogging {

    import bootstrap.State.State

    val boot: NegativePort[Bootstrapping] = provides[Bootstrapping]
    var network: PositivePort[Network] = requires[Network]
    var timer: PositivePort[Timer] = requires[Timer]

    val self: TAddress = cfg.getValue[TAddress]("stormy.address")
    val bootThreshold: Int = cfg.getValue[Int]("stormy.bootThreshold")

    private var state: State = State.Collecting
    private var timeoutId: Option[UUID] = None

    private val active: collection.mutable.Set[TAddress] = collection.mutable.Set()
    private val ready: collection.mutable.Set[TAddress] = collection.mutable.Set()

    private var initialAssignment: Option[NodeAssignment] = None

    ctrl uponEvent {
        case _: Start => handle {
            logger.info(s"Starting bootstrap server on $self with boot threshold $bootThreshold")
            val timeout: Long = cfg.getValue[Long]("stormy.keepAlivePeriod") * 2
            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(new BootstrapTimeout(spt))
            trigger(spt -> timer)
            timeoutId = Some(spt.getTimeoutEvent.getTimeoutId)
            active.add(self)
        }
    }

    timer uponEvent {
        case _: BootstrapTimeout => handle {
            state match {
                case State.Collecting =>
                    logger.info(s"${active.size} hosts in active set")
                    if (active.size >= bootThreshold) {
                        bootUp()
                    }
                case State.Seeding =>
                    logger.info(s"${ready.size} hosts in ready set")
                    if (ready.size >= bootThreshold) {
                        logger.info("Finished seeding. Bootstrapping completed")
                        trigger(Booted(initialAssignment.get) -> boot)
                        state = State.Done
                    }
                case State.Done =>
                    logger.info("In DONE state, committing suicide")
                    suicide()
            }
        }
    }

    boot uponEvent {
        case ev: InitialAssignments => handle {
            logger.info("Seeding assignments")
            logger.debug(s"${ev.assignment}")
            initialAssignment = Some(ev.assignment)
            for (it <- active) trigger(TMessage(self, it, Boot(initialAssignment.get)) -> network)
            ready.add(self)
        }
    }

    network uponEvent {
        case TMessage(source, self, CheckIn) => handle {
            active.add(source)
        }
        case TMessage(source, self, Ready) => handle {
            ready.add(source)
        }
        case x => handle {
            logger.info(s"got: $x")
        }
    }

    private def bootUp() = {
        logger.info("Threshold reached. Generating assignments now.")
        state = State.Seeding
        //  TODO Verify correctness of below. It's ugly
        val clone = collection.immutable.Set() ++ active
        trigger(GetInitialAssignments(clone) -> boot)
    }

    override def tearDown(): Unit = {
        trigger(new CancelPeriodicTimeout(timeoutId.get) -> timer)
    }
}

class BootstrapTimeout(sbt: SchedulePeriodicTimeout) extends Timeout(sbt)
