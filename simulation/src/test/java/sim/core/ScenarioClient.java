package sim.core;/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
import stormy.kv.OperationResponse;
import stormy.networking.NetAddress;
import stormy.networking.NetMessage;
import stormy.overlay.RouteMessage;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Lars Kroll <lkroll@kth.se>
 */
public class ScenarioClient extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(ScenarioClient.class);
    //******* Ports ******

    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    //******* Fields ******

    private final NetAddress self = config().getValue("stormy.address", NetAddress.class);
    private final NetAddress server = config().getValue("stormy.coordinatorAddress", NetAddress.class);

    private final SimulationResultMap res = SimulationResultSingleton.getInstance();
    private final Map<String, String> pending = new TreeMap<>();

    //******* Handlers ******

    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            int messages = res.get("messages", Integer.class);
            for (int i = 0; i < messages; i++) {
                Operation op = new GetOperation("test" + i, "client_id" + i, self);
                RouteMessage rm = new RouteMessage(op.key(), op); // don't know which partition is responsible, so ask the bootstrap server to forward it
                pending.put(op.id(), op.key());
                LOG.info("Sending {}", op);
                res.put(op.key(), "SENT");
                trigger(new NetMessage<>(self, server, rm), net);
            }
        }
    };

    protected final Handler<NetMessage> handler = new Handler<NetMessage>() {
        @Override
        public void handle(NetMessage event) {
            LOG.debug("Got OperationResponse: {}", event.payload());
            if (event.payload() instanceof OperationResponse) {
                OperationResponse content = (OperationResponse) event.payload();
                String key = pending.remove(content.id());
                if (key != null) {
                    res.put(key, content.status().toString());
                } else {
                    LOG.warn("ID {} was not pending! Ignoring response.", content.id());
                }
            }
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(handler, net);
    }
}
