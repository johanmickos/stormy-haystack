package sim.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import stormy.kv.GetOperation;
import stormy.kv.Operation;
import stormy.kv.PutOperation;
import stormy.networking.NetAddress;
import stormy.networking.NetMessage;
import stormy.overlay.RouteMessage;

import java.util.UUID;

/**
 * Created by khaled on 3/3/17.
 */
public class ClientPut extends ComponentDefinition {
    final static Logger LOG = LoggerFactory.getLogger(ScenarioClient.class);

    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    private final NetAddress server = config().getValue("stormy.coordinatorAddress", NetAddress.class);
    private final NetAddress self = config().getValue("stormy.address", NetAddress.class);

    public String key;
    public String value;

    public ClientPut(Init init){
        this.key = init.key;
        this.value = init.value;
    }

    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            Operation op = new PutOperation(key, value, UUID.randomUUID().toString(), self);
            RouteMessage rm = new RouteMessage(op.key(), op);
            trigger(new NetMessage<>(self, server, rm), net);
            LOG.info("Sending {}", op);
        }
    };

    {
        subscribe(startHandler, control);
    }

    public static class Init extends se.sics.kompics.Init<ClientPut> {
        public String key;
        public String value;

        public Init(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
