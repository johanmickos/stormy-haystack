package overlay

import bootstrap.{Booted, Bootstrapping, GetInitialAssignments, InitialAssignments}
import com.typesafe.scalalogging.StrictLogging
import ex.{TAddress, TMessage}
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort, _}
import se.sics.kompics.timer.Timer

class VSOverlayManager extends ComponentDefinition with StrictLogging {
    val routing: NegativePort[Routing] = provides[Routing]

    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]
    val bootstrap: PositivePort[Bootstrapping] = requires[Bootstrapping]

    val self: TAddress = cfg.getValue[TAddress]("stormy.address")

    private var lut: Option[PartitionLookupTable] = None

    bootstrap uponEvent {
        case GetInitialAssignments(nodes) => handle {
            logger.info("Generating lookup table")
            lut = Some(new PartitionLookupTable(nodes))
            logger.debug(s"Generated LUT: ${lut.get}")
            trigger(InitialAssignments(lut.get) -> bootstrap)
        }
        case Booted(update: PartitionLookupTable) => handle {
            logger.info("Got node assignment. Overlay ready")
            lut = Some(update)
            trigger(InitialAssignments(lut.get) -> bootstrap)
        }
    }

    network uponEvent {
        case TMessage(source, self, payload: RouteMessage) => handle {
            logger.info("Received route message")
            // TODO Check that lut.get() doesn't return None
            val partitions = lut.get.lookup(payload.key)
            val randomTarget:TAddress = partitions.get.toList.head
            logger.info(s"Forwarding message to random target ${randomTarget.getIp()}")
            trigger(TMessage(self, randomTarget, payload) -> network)
        }
        case TMessage(source, self, payload: Connect) => handle {
            lut match {
                case Some(_) =>
                    logger.debug(s"Accepting connection request from $source")
                    val size: Int = lut.get.getNodes.size
                    trigger(TMessage(self, source, Ack(payload.id, size)) -> network)
                case None =>
                    logger.warn(s"Rejecting connection request from $source as system is not ready yet")
            }
        }
    }

    routing uponEvent {
        case TMessage(source, self, payload: RouteMessage) => handle {
            logger.info("Received local route message")
            // TODO Check that lut.get() doesn't return None
            val partitions = lut.get.lookup(payload.key)
            val randomTarget:TAddress = partitions.get.toList.head
            logger.info(s"Routing message for key ${payload.key} to $randomTarget")
            trigger(TMessage(self, randomTarget, payload.msg) -> network)
        }
    }

}
