package sim.wrapper;

import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import stormy.components.eld.Omega;

/**
 * Created by khaled on 2/25/17.
 */
public class EldParent extends ComponentDefinition {
    private final Positive<Network> network = requires(Network.class);
    private final Positive<Timer> timer = requires(Timer.class);

    public EldParent() {
        //create and connect all components except timer and network
        Component eld = create(Omega.class, Init.NONE);

        //connect required internal components to network and timer
        connect(eld.getNegative(Network.class), network, Channel.TWO_WAY);
        connect(eld.getNegative(Timer.class), timer, Channel.TWO_WAY);
    }
}
