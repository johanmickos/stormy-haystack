package ex

import com.typesafe.scalalogging.StrictLogging
import se.sics.kompics.network.{Network, Transport}
import se.sics.kompics.sl._
import se.sics.kompics.timer.Timer
import test.{Ping, Pong}

class Ponger extends ComponentDefinition with StrictLogging {

        val net = requires[Network]
        val timer = requires[Timer]

        private var counter: Long = 0
        private val self = cfg.getValue[TAddress]("pingpong.self")

        net uponEvent {
            case context@TMessage(source, self, Ping) => handle {
                counter += 1
                logger.info(s"Got Ping #$counter!")
                trigger(TMessage(self, source, Pong) -> net)
            }
            case x => handle {
                logger.info(s"Got unknown: $x")
            }
        }
    }