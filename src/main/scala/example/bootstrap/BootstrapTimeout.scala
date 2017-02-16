package example.bootstrap

import se.sics.kompics.timer.{SchedulePeriodicTimeout, Timeout}

case class BootstrapTimeout(spt: SchedulePeriodicTimeout) extends Timeout(spt) {}
