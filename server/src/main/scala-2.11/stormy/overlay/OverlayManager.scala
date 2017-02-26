package stormy.overlay

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort, _}
import se.sics.kompics.timer.Timer
import stormy.bootstrap.{Booted, Bootstrapping, GetInitialAssignments, InitialAssignments}
import stormy.components.epfd.EPDFSpec.EventuallyPerfectFailureDetector
import stormy.networking.{NetAddress, NetMessage}

import scala.util.Random

class OverlayManager extends ComponentDefinition with StrictLogging {
    val routing: NegativePort[Routing] = provides[Routing]

    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]
    val bootstrap: PositivePort[Bootstrapping] = requires[Bootstrapping]
    val epfd: PositivePort[EventuallyPerfectFailureDetector] = requires[EventuallyPerfectFailureDetector]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
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
            trigger(OverlayUpdate(update.getNodes) -> routing)
        }
    }

    network uponEvent {
        case ctx@NetMessage(source, self, payload: RouteMessage) => handle {
            logger.info(s"Received route message: ${payload.msg.toString}")
            // TODO Check that lut.get() doesn't return None
            val partitions = lut.get.lookup(payload.key)
            // TODO Broadcast message to its replication group
            val rnd = new Random()
            val randomTarget: NetAddress = partitions.toVector(rnd.nextInt(partitions.size))
            logger.info(s"Forwarding message to random target ${randomTarget.getIp()}:${randomTarget.getPort}")
            trigger(NetMessage(source, randomTarget, payload.msg) -> network)
        }
        case NetMessage(source, self, payload: Connect) => handle {
            lut match {
                case Some(_) =>
                    logger.debug(s"Accepting connection request from $source with ID ${payload.id}")
                    val size: Int = lut.get.getNodes.size
                    trigger(NetMessage(self, source, Ack(payload.id, size)) -> network)
                case None =>
                    logger.warn(s"Rejecting connection request from $source as system is not ready yet")
            }
        }
    }

    routing uponEvent {
        case NetMessage(source, self, payload: RouteMessage) => handle {
            logger.info("Received local route message")
            // TODO Check that lut.get() doesn't return None
            val partitions = lut.get.lookup(payload.key)
            val randomTarget: NetAddress = partitions.toList.head
            logger.info(s"Routing message for key ${payload.key} to $randomTarget")
            trigger(NetMessage(self, randomTarget, payload.msg) -> network)
        }
    }

}

