package overlay

import java.util

import se.sics.kompics.sl._
import bootstrap.{Booted, Bootstrapping, GetInitialAssignments}
import com.typesafe.scalalogging.StrictLogging
import ex.{TAddress, TMessage}
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, PositivePort}
import se.sics.kompics.timer.Timer

import scala.util.Random

class VSOverlayManager extends ComponentDefinition with StrictLogging {
    val network: PositivePort[Network] = requires[Network]
    val timer: PositivePort[Timer] = requires[Timer]
    val boot: PositivePort[Bootstrapping] = requires[Bootstrapping]
    val routing: NegativePort[Routing] = provides[Routing]

    val self: TAddress = cfg.getValue[TAddress]("stormy.address")

    private var lut: Option[PartitionLookupTable] = None

    boot uponEvent {
        case GetInitialAssignments(nodes) => handle {
            logger.info("Generating lookup table")
            lut = Some(new PartitionLookupTable(nodes))
        }
        case Booted(update: PartitionLookupTable) => handle {
            logger.info("Got node assignment. Overlay ready")
            lut = Some(update)
        }
    }

    network uponEvent {
        case TMessage(source, self, payload: RouteMessage) => handle {
            logger.info("Received route message")
            // TODO Check that lut.get() doesn't return None
            val partition: util.Collection[TAddress] = lut.get.lookup(payload.key)
            val randomTarget:TAddress = Random.shuffle(partition).iterator().next()
            logger.info(s"Forwarding message to random target ${randomTarget.getIp()}")
            trigger(TMessage(self, randomTarget, payload) -> network)
        }
        case TMessage(source, self, payload: Connect) => handle {
            lut match {
                case Some(_) =>
                    logger.debug(s"Accepting connection request from $source")
                    val size: Int = lut.get.getNodes.size()
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
            val partition: util.Collection[TAddress] = lut.get.lookup(payload.key)
            val randomTarget:TAddress = Random.shuffle(partition).iterator().next()
            logger.info(s"Routing message for key ${payload.key} to $randomTarget")
            trigger(TMessage(self, randomTarget, payload.msg) -> network)
        }
    }

}
