package kvstore

import com.google.common.util.concurrent.SettableFuture
import com.typesafe.scalalogging.StrictLogging
import ex.{TAddress, TMessage}
import kv.{Operation, OperationResponse}
import overlay.{Ack, Connect, RouteMessage}
import se.sics.kompics.network.Network
import se.sics.kompics.sl._
import se.sics.kompics.timer.{SchedulePeriodicTimeout, Timeout, Timer}
import se.sics.kompics.{Kompics, KompicsEvent, Start}


class ClientService extends ComponentDefinition with StrictLogging {
    var network: PositivePort[Network] = requires[Network]
    var timer: PositivePort[Timer] = requires[Timer]

    val self: TAddress = config.getValue("stormy.address", classOf[TAddress])
    val coordinator: TAddress = config.getValue("stormy.coordinatorAddress", classOf[TAddress])

    private var connected: Option[Ack] = None
    private val pending = new java.util.TreeMap[String, SettableFuture[OperationResponse]]()


    ctrl uponEvent {
        case _: Start => handle {
            logger.info(s"Starting client at $self")
            val timeout: Long = cfg.getValue[Long]("stormy.keepAlivePeriod") * 2
            val spt = new SchedulePeriodicTimeout(timeout, timeout)
            spt.setTimeoutEvent(ConnectTimeout(spt))
            logger.debug(s"Setting up with timeout ID ${spt.getTimeoutEvent.getTimeoutId}")
            trigger(TMessage(self, coordinator, Connect(spt.getTimeoutEvent.getTimeoutId.toString)) -> network)
            trigger(spt -> timer)
        }
    }

    loopbck uponEvent {
        case ctx@OpWithFuture(op) => handle {
            val msg: RouteMessage = RouteMessage(op.key, op) // don't know which partition is responsible, so ask the bootstrap server to forward it
            trigger(TMessage(self, coordinator, msg) -> network)
            pending.put(op.id, ctx.sf)
        }
    }

    network uponEvent {
        case TMessage(source, self, ack: Ack) => handle {
            logger.info(s"Client connected to $source, cluster size is ${ack.clusterSize}")
            connected = Some(ack)
            val console: Console = new Console(ClientService.this)
            val th: Thread = new Thread(console)
            th.start()
        }
        case TMessage(source, self, response: OperationResponse) => handle {
            logger.debug(s"Received respoonse $response")
            val sf: Option[SettableFuture[OperationResponse]] = Some(pending.remove(response.id))
            sf match {
                case Some(value) =>
                    value.set(response)
                    // TODO Why do we set this? Do we have a ref. to it anywhere else?
                case None => logger.warn(s"Operation ID ${response.id} was not pending! Ignoring response.")
            }
        }
    }

    timer uponEvent {
        case ev: ConnectTimeout => handle {
            logger.info(s"Received event: $ev\n${connected.get}\n${connected.get.id}\n${ev.getTimeoutId}")
            connected match {
                case Some(ack: Ack) =>
                    if (!ack.id.equals(ev.getTimeoutId.toString)) {
                        logger.error("Received wrong response ID earlier! System may be inconsistent. Shutting down")
                        System.exit(1)
                    }
                case None =>
                    logger.error(s"Connection to server $coordinator did not succeed. Shutting down.")
                    Kompics.asyncShutdown()
            }
        }
    }

    private[kvstore] def op(key: String) = {
        val op = new Operation(key, Operation.genId())
        val owf = new OpWithFuture(op)
        trigger(owf, onSelf)
        owf.sf
    }

}

case class ConnectTimeout(spt: SchedulePeriodicTimeout) extends Timeout(spt)
case class OpWithFuture(op: Operation) extends KompicsEvent {
    val sf: SettableFuture[OperationResponse] = SettableFuture.create[OperationResponse]
}