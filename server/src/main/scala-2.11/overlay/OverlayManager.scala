package overlay

import bootstrap.{Booted, Bootstrapping, GetInitialAssignments, InitialAssignments}
import com.typesafe.scalalogging.StrictLogging
import networking.{TAddress, TMessage}
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort, _}
import se.sics.kompics.timer.Timer

import scala.util.Random

class OverlayManager extends ComponentDefinition with StrictLogging {
    val routing: NegativePort[Routing] = provides[Routing]

    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]
    val bootstrap: PositivePort[Bootstrapping] = requires[Bootstrapping]

    val self: TAddress = cfg.getValue[TAddress]("stormy.address")
    val replicationFactor: Int = cfg.getValue[Int]("stormy.replicationFactor")

    private var lut: Option[PartitionLookupTable] = None

    bootstrap uponEvent {
        case GetInitialAssignments(nodes) => handle {
            logger.info("Generating lookup table")
            val plut = new PartitionLookupTable(replicationFactor)
            plut.generate(nodes)
            if (plut.isUnderReplicated) {
                logger.warn("System is under-replicated")
            }
            logger.debug(s"Generated LUT: ${plut}")
            trigger(InitialAssignments(plut) -> bootstrap)
        }
        case Booted(update: PartitionLookupTable) => handle {
            logger.info("Got node assignment. Overlay ready")
            logger.debug(s"${update.getNodes}")
            lut = Some(update)
        }
    }

    network uponEvent {
        case ctx@TMessage(source, self, payload: RouteMessage) => handle {
            logger.info(s"Received route message: ${payload.msg.toString}")
            // TODO Check that lut.get() doesn't return None
            val partitions = lut.get.lookup(payload.key)
            // TODO Broadcast message to its replication group
            val rnd = new Random()
            val randomTarget:TAddress = partitions.toVector(rnd.nextInt(partitions.size))
            logger.info(s"Forwarding message to random target ${randomTarget.getIp()}:${randomTarget.getPort}")
            trigger(TMessage(source, randomTarget, payload.msg) -> network)
        }
        case TMessage(source, self, payload: Connect) => handle {
            lut match {
                case Some(_) =>
                    logger.debug(s"Accepting connection request from $source with ID ${payload.id}")
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
            val randomTarget:TAddress = partitions.toList.head
            logger.info(s"Routing message for key ${payload.key} to $randomTarget")
            trigger(TMessage(self, randomTarget, payload.msg) -> network)
        }
    }

}
