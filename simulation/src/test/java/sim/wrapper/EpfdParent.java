package sim.wrapper;

import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import stormy.components.epfd.EPFD;
import stormy.networking.NetAddress;

/**
 * Created by khaled on 2/25/17.
 */
public class EpfdParent extends ComponentDefinition {

    private final Positive<Network> network = requires(Network.class);
    private final Positive<Timer> timer = requires(Timer.class);

    public EpfdParent(EpfdInit init) {
        //create and connect all components except timer and network
        Component epfd = create(EPFD.class, Init.NONE);

        //connect required internal components to network and timer
        connect(epfd.getNegative(Network.class), network, Channel.TWO_WAY);
        connect(epfd.getNegative(Timer.class), timer, Channel.TWO_WAY);
    }

    public static class EpfdInit extends se.sics.kompics.Init<EPFD> {
        NetAddress netAddress;
        public EpfdInit(NetAddress netAddress) {
            this.netAddress = netAddress;
        }
    }

}
