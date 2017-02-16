package ex

import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.Start
import se.sics.kompics.network.{Network, Transport}
import se.sics.kompics.sl._
import se.sics.kompics.timer.{CancelPeriodicTimeout, SchedulePeriodicTimeout, Timeout, Timer}
import test.{Ping, Pong}

case class PingTimeout(spt: SchedulePeriodicTimeout) extends Timeout(spt)

class Pinger(init: Init[Pinger]) extends ComponentDefinition with StrictLogging {
    val net = requires[Network]
    val timer = requires[Timer]

    private val self = cfg.getValue[TAddress]("pingpong.self")
    private val ponger = init match {
        case Init(pongerAddr: TAddress) => pongerAddr
    }
    private var counter: Long = 0
    private var timerId: Option[UUID] = None

    ctrl uponEvent {
        case _: Start => handle {
            val period = cfg.getValue[Long]("pingpong.pinger.timeout")
            logger.info(s"Starting pinger with timeout $period")
            val spt = new SchedulePeriodicTimeout(0, period)
            val timeout = PingTimeout(spt)
            spt.setTimeoutEvent(timeout)
            trigger(spt -> timer)
            timerId = Some(timeout.getTimeoutId)
            logger.info(s"Triggered SPT on timer with timeout ID: $timerId")
        }
    }

    net uponEvent {
        case context@TMessage(_, Pong) => handle {
            counter += 1
            logger.info(s"Got Pong #$counter!")
        }
        case x => handle {
            logger.info(s"Got unknown: ${x}")
        }
    }

    timer uponEvent {
        case PingTimeout(_) => handle {
            logger.info("Sending ping!")
            trigger(TMessage(THeader(self, ponger, Transport.TCP), Ping) -> net)
        }
    }

    override def tearDown(): Unit = {
        timerId match {
            case Some(id) =>
                trigger(new CancelPeriodicTimeout(id) -> timer)
            case None => // nothing
        }
    }
}