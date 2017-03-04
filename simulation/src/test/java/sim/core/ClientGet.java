package sim.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import stormy.kv.GetOperation;
import stormy.kv.Operation;
import stormy.kv.OperationResponse;
import stormy.networking.NetAddress;
import stormy.networking.NetMessage;
import stormy.overlay.RouteMessage;

import java.util.UUID;

public class ClientGet extends ComponentDefinition {
    final static Logger LOG = LoggerFactory.getLogger(ScenarioClient.class);

    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    private final NetAddress server = config().getValue("stormy.coordinatorAddress", NetAddress.class);
    private final NetAddress self = config().getValue("stormy.address", NetAddress.class);

    public String key;

    public ClientGet(Init init) {
        this.key = init.key;
    }

    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            Operation op = new GetOperation(key, UUID.randomUUID().toString(), self);
            RouteMessage rm = new RouteMessage(op.key(), op);
            trigger(new NetMessage<>(self, server, rm), net);
            LOG.info("Sending {}", op);
        }
    };

    protected final Handler<NetMessage> handler = new Handler<NetMessage>() {
        @Override
        public void handle(NetMessage event) {
            LOG.debug("Got OperationResponse: {}", event.payload());
            if (event.payload() instanceof OperationResponse) {
                OperationResponse content = (OperationResponse) event.payload();
                String key = content.id();
                if (key != null) {
                    LOG.info("Got key: " + key);
                } else {
                    LOG.warn("Key is not available");
                }
            }
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(handler, net);
    }

    public static class Init extends se.sics.kompics.Init<ClientGet> {
        public String key;

        public Init(String key) {
            this.key = key;
        }
    }
}
