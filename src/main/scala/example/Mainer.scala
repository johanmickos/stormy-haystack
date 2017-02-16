package example

import se.sics.kompics.Kompics

object Mainer {
    def main(args: Array[String]): Unit = {
//        cb.addValue("host.address", new NetAddress(InetAddress.getByName("http://localhost"), 8082))
        Kompics.createAndStart(classOf[HostComponent])
    }
}
