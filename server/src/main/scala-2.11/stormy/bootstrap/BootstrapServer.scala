package stormy.bootstrap

import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Start
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, NegativePort, PositivePort, _}
import se.sics.kompics.timer.{CancelPeriodicTimeout, SchedulePeriodicTimeout, Timeout, Timer}
import stormy.bootstrap.State.State
import stormy.networking.{NetAddress, NetMessage}
import stormy.overlay.PartitionLookupTable

object State extends Enumeration {
    type State = Value
    val Collecting, Seeding, Done = Value
}

class BootstrapServer extends ComponentDefinition with StrictLogging {

    val boot: NegativePort[Bootstrapping] = provides[Bootstrapping]

    var network: PositivePort[Network] = requires[Network]
    var timer: PositivePort[Timer] = requires[Timer]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    val bootThreshold: Int = cfg.getValue[Int]("stormy.bootThreshold")
    private val active: collection.mutable.Set[NetAddress] = collection.mutable.Set()
    private val ready: collection.mutable.Set[NetAddress] = collection.mutable.Set()

    private var state: State = State.Collecting
    private var timeoutId: Option[String] = None
    private var initialAssignment: Option[PartitionLookupTable] = None

    ctrl uponEvent {
        case _: Start => handle {
            logger.info(s"Starting bootstrap server on $self with boot threshold $bootThreshold")
            val timeout: Long = cfg.getValue[Long]("stormy.keepAlivePeriod") * 2
            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(new BootstrapTimeout(spt))
            trigger(spt -> timer)
            timeoutId = Some(spt.getTimeoutEvent.getTimeoutId.toString)
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
                    suicide()
            }
        }
    }

    boot uponEvent {
        case ev: InitialAssignments => handle {
            logger.info("Seeding assignments")
            logger.debug(s"${ev.assignment}")
            initialAssignment = Some(ev.assignment)
            for (it <- active) trigger(NetMessage(self, it, Boot(initialAssignment.get)) -> network)
            ready.add(self)
        }
    }

    network uponEvent {
        case NetMessage(source, self, CheckIn) => handle {
            active.add(source)
        }
        case NetMessage(source, self, Ready) => handle {
            ready.add(source)
        }
        case x => handle {
            logger.info(s"got: $x")
        }
    }

    override def tearDown(): Unit = {
        trigger(new CancelPeriodicTimeout(UUID.fromString(timeoutId.get)) -> timer)
    }

    private def bootUp() = {
        logger.info("Threshold reached. Generating assignments now.")
        state = State.Seeding
        //  TODO Verify correctness of below. It's ugly
        val clone = collection.immutable.Set() ++ active
        trigger(GetInitialAssignments(clone) -> boot)
    }
}

class BootstrapTimeout(sbt: SchedulePeriodicTimeout) extends Timeout(sbt)
