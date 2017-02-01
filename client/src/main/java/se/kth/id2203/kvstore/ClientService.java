/*
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
package se.kth.id2203.kvstore;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Future;
import org.slf4j.LoggerFactory;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Connect;
import se.kth.id2203.overlay.RouteMsg;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class ClientService extends ComponentDefinition {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ClientService.class);
    //******* Ports ******
    final Positive<Timer> timer = requires(Timer.class);
    final Positive<Network> net = requires(Network.class);
    //******* Fields ******
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final NetAddress server = config().getValue("id2203.project.bootstrap-address", NetAddress.class);
    private Optional<Connect.Ack> connected = Optional.absent();
    private final Map<UUID, SettableFuture<OpResponse>> pending = new TreeMap<>();

    //******* Handlers ******
    protected final Handler<Start> startHandler = new Handler<Start>() {
        
        @Override
        public void handle(Start event) {
            LOG.debug("Starting client on {}. Waiting to connect...", self);
            long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 2);
            ScheduleTimeout st = new ScheduleTimeout(timeout);
            st.setTimeoutEvent(new ConnectTimeout(st));
            trigger(new Message(self, server, new Connect(st.getTimeoutEvent().getTimeoutId())), net);
            trigger(st, timer);
        }
    };
    protected final ClassMatchedHandler<Connect.Ack, Message> connectHandler = new ClassMatchedHandler<Connect.Ack, Message>() {
        
        @Override
        public void handle(Connect.Ack content, Message context) {
            LOG.info("Client connected to {}, cluster size is {}", server, content.clusterSize);
            connected = Optional.of(content);
            Console c = new Console(ClientService.this);
            Thread tc = new Thread(c);
            tc.start();
        }
    };
    protected final Handler<ConnectTimeout> timeoutHandler = new Handler<ConnectTimeout>() {
        
        @Override
        public void handle(ConnectTimeout event) {
            if (!connected.isPresent()) {
                LOG.error("Connection to server {} did not succeed. Shutting down...", server);
                Kompics.asyncShutdown();
            } else {
                Connect.Ack cack = connected.get();
                if (!cack.id.equals(event.getTimeoutId())) {
                    LOG.error("Received wrong response id earlier! System may be inconsistent. Shutting down...", server);
                    System.exit(1);
                }
            }
        }
    };
    protected final Handler<OpWithFuture> opHandler = new Handler<OpWithFuture>() {
        
        @Override
        public void handle(OpWithFuture event) {
            RouteMsg rm = new RouteMsg(event.op.key, event.op); // don't know which partition is responsible, so ask the bootstrap server to forward it
            trigger(new Message(self, server, rm), net);
            pending.put(event.op.id, event.f);
        }
    };
    protected final ClassMatchedHandler<OpResponse, Message> responseHandler = new ClassMatchedHandler<OpResponse, Message>() {
        
        @Override
        public void handle(OpResponse content, Message context) {
            LOG.debug("Got OpResponse: {}", content);
            SettableFuture<OpResponse> sf = pending.remove(content.id);
            if (sf != null) {
                sf.set(content);
            } else {
                LOG.warn("ID {} was not pending! Ignoring response.", content.id);
            }
        }
    };
    
    {
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(connectHandler, net);
        subscribe(opHandler, loopback);
        subscribe(responseHandler, net);
    }
    
    Future<OpResponse> op(String key) {
        Operation op = new Operation(key);
        OpWithFuture owf = new OpWithFuture(op);
        trigger(owf, onSelf);
        return owf.f;
    }
    
    public static class OpWithFuture implements KompicsEvent {
        
        public final Operation op;
        public final SettableFuture<OpResponse> f;
        
        public OpWithFuture(Operation op) {
            this.op = op;
            this.f = SettableFuture.create();
        }
    }
    
    public static class ConnectTimeout extends Timeout {
        
        ConnectTimeout(ScheduleTimeout st) {
            super(st);
        }
    }
}
