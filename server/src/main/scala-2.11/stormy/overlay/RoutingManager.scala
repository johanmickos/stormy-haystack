package stormy.overlay

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.network.Network
import se.sics.kompics.sl.{ComponentDefinition, _}
import se.sics.kompics.timer.Timer
import stormy.bootstrap.{Booted, Bootstrapping, GetInitialAssignments, InitialAssignments}
import stormy.components.Ports.{BEB_Broadcast, BestEffortBroadcast}
import stormy.components.epfd.EPDFSpec.{EventuallyPerfectFailureDetector, Restore, Suspect}
import stormy.kv.{Operation, OperationResponse}
import stormy.networking.{NetAddress, NetMessage}

import scala.util.Random


class RoutingManager extends ComponentDefinition with StrictLogging {
    val routing: NegativePort[Routing] = provides[Routing]

    val network = requires[Network]
    val timer = requires[Timer]
    val bootstrap = requires[Bootstrapping]
    val epfd = requires[EventuallyPerfectFailureDetector]
    val beb = requires[BestEffortBroadcast]

    val self: NetAddress = cfg.getValue[NetAddress]("stormy.address")
    val replicationFactor: Int = cfg.getValue[Int]("stormy.replicationFactor")

    var suspected: Set[NetAddress] = Set()

    private var lut: Option[PartitionLookupTable] = None
    private val rnd = new Random()


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
            trigger(OverlayUpdate(update) -> routing)
        }
    }

    epfd uponEvent {
        case Suspect(node: NetAddress) => handle {
            suspected = suspected + node
        }
        case Restore(node: NetAddress) => handle {
            suspected = suspected - node
        }
    }

    network uponEvent {
        case ctx@NetMessage(source, self, opResponse: OperationResponse) => handle {
            logger.debug("Received response from cluster. Verifying majority vote.")
            // TODO
            trigger(NetMessage(source, opResponse.operation.client, opResponse) -> network)
        }
        // Below catches BEB_Broadcast to prevent having to have a
        // BEB component for each replication group
        case ctx@NetMessage(source, self, BEB_Broadcast(op: Operation)) => handle {
            trigger(op -> routing)
        }
        case ctx@NetMessage(source, self, payload: RouteMessage) => handle {
            logger.info(s"Received route message: ${payload.msg.toString}")
            // TODO Check that lut.get() doesn't return None
            val replicationGroup = lut.get.lookup(payload.key)

            val alive = replicationGroup.diff(suspected)
            val randomLiveNode = alive.toVector(rnd.nextInt(alive.size))
            logger.debug(s"$self forwarding $payload to $randomLiveNode in $replicationGroup")
            trigger(NetMessage(source, randomLiveNode, BEB_Broadcast(payload.msg)) -> network)
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

