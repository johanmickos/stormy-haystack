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
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation1;
import se.sics.kompics.simulator.adaptor.Operation2;
import se.sics.kompics.simulator.adaptor.distributions.ConstantDistribution;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.KillNodeEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;
import stormy.ParentComponent;
import stormy.kv.GetOperation;
import stormy.kv.KVService;
import stormy.kv.Operation;
import stormy.networking.NetAddress;
import stormy.networking.NetMessage;

import java.net.InetSocketAddress;
import java.util.*;

public abstract class ScenarioGen {
    private final static Logger logger = LoggerFactory.getLogger(ScenarioGen.class);

    private static final Operation1 startServerOp = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress selfAdr;
                final NetAddress bsAdr;

                {
                    try {
                        selfAdr = new NetAddress(new InetSocketAddress("192.168.0." + self, 45678));
                        bsAdr = new NetAddress(new InetSocketAddress("192.168.0.1", 45678));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return ParentComponent.class;
                }

                @Override
                public String toString() {
                    return "StartNode<" + selfAdr.toString() + ">";
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("stormy.address", selfAdr);
                    config.put("stormy.coordinatorAddress", bsAdr);

                    if (self != 1) {
                        config.put("stormy.type", "server");
                    } else {
                        config.put("stormy.type", "coordinator");
                    }
                    return config;
                }
            };
        }
    };

    private static final Operation1 startClientOp = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress selfAdr;
                final NetAddress bsAdr;

                {
                    try {
                        selfAdr = new NetAddress(new InetSocketAddress("192.168.1." + self, 45678));
                        bsAdr = new NetAddress(new InetSocketAddress("192.168.0.1", 45678));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public NetAddress getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return client.ParentComponent.class;
                }

                @Override
                public String toString() {
                    return "StartClient<" + selfAdr.toString() + ">";
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("stormy.address", selfAdr);
                    config.put("stormy.type", "client");
                    config.put("stormy.coordinatorAddress", bsAdr);
                    return config;
                }
            };
        }
    };

    static Operation1 getOp = new Operation1<StartNodeEvent, Integer>() {
        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress bsAdr;
                final NetAddress selfAdr;

                {
                    bsAdr = new NetAddress(new InetSocketAddress("192.168.0.1", 45678));
                    selfAdr = new NetAddress(new InetSocketAddress("192.168.1." + self, 45678));
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class<? extends ComponentDefinition> getComponentDefinition() {
                    return ClientGet.class;
                }

                @Override
                public Init getComponentInit() {
                    return new ClientGet.Init(selfAdr.getIp().toString());
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("stormy.address", selfAdr);
                    config.put("stormy.type", "client");
                    config.put("stormy.coordinatorAddress", bsAdr);
                    return config;
                }
            };
        }
    };

    static Operation1 putOp = new Operation1<StartNodeEvent, Integer>() {
        @Override
        public StartNodeEvent generate(final Integer self) {
            return new StartNodeEvent() {
                final NetAddress bsAdr;
                final NetAddress selfAdr;

                {
                    bsAdr = new NetAddress(new InetSocketAddress("192.168.0.1", 45678));
                    selfAdr = new NetAddress(new InetSocketAddress("192.168.1." + self, 45678));
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class<? extends ComponentDefinition> getComponentDefinition() {
                    return ClientPut.class;
                }

                @Override
                public Init getComponentInit() {
                    return new ClientPut.Init(selfAdr.getIp().toString(), self.toString()); // we can change the value to something else later
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("stormy.address", selfAdr);
                    config.put("stormy.type", "client");
                    config.put("stormy.coordinatorAddress", bsAdr);
                    return config;
                }
            };
        }
    };

    static Operation2 casOp = new Operation2<StartNodeEvent, Integer, Integer>() {
        @Override
        public StartNodeEvent generate(final Integer self, Integer valType) {
            return new StartNodeEvent() {
                final NetAddress bsAdr;
                final NetAddress selfAdr;

                {
                    bsAdr = new NetAddress(new InetSocketAddress("192.168.0.1", 45678));
                    selfAdr = new NetAddress(new InetSocketAddress("192.168.1." + self, 45678));
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class<? extends ComponentDefinition> getComponentDefinition() {
                    return ClientCas.class;
                }

                @Override
                public Init getComponentInit() {
                    if(valType.equals(0)) { // engage correct refValue
                        return new ClientCas.Init(selfAdr.getIp().toString(), self.toString(), ((Integer) (self + 1000)).toString());
                    } else {
                        return new ClientCas.Init(selfAdr.getIp().toString(), "incorrectVal", ((Integer) (self + 1000)).toString());
                    }
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("stormy.address", selfAdr);
                    config.put("stormy.type", "client");
                    config.put("stormy.coordinatorAddress", bsAdr);
                    return config;
                }
            };
        }
    };

    public static SimulationScenario simpleOps(final int servers) {
        return new SimulationScenario() {
            {
                SimulationScenario.StochasticProcess startCluster = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(servers, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };

                SimulationScenario.StochasticProcess startClients = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                startCluster.start();
                startClients.startAfterTerminationOf(50000, startCluster);
                terminateAfterTerminationOf(500000, startClients);
            }
        };
    }

    static Operation1 killNode = new Operation1<KillNodeEvent, Integer>() {
        @Override
        public KillNodeEvent generate(final Integer self) {
            logger.info("Generating KillNodeEvent");
            return new KillNodeEvent() {
                final NetAddress selfAdr;

                {
                    selfAdr = new NetAddress(new InetSocketAddress("192.168.0." + self, 45678));
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public String toString() {
                    return "Kill node <" + selfAdr.toString() + ">";
                }
            };
        }
    };

    /**
     * Simulation Scenario to test eventually perfect failure detector properties
     * 1.	EPFD1: Strong completeness: Every crashed process is eventually detected by all correct processes
     * 2.	EPFD2: Eventual strong accuracy: Eventually, no correct process is suspected by any correct process.
     */
    public static SimulationScenario testEPFD_Properties() {
        SimulationScenario testEPFD = new SimulationScenario() {
            {

                SimulationScenario.StochasticProcess srvNodes = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(500));
                        raise(5, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };

                StochasticProcess killNode1 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(2));
                    }
                };

                StochasticProcess killNode2 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(5));
                    }
                };

                srvNodes.start();
                killNode1.startAfterTerminationOf(5000, srvNodes);
                killNode2.startAfterTerminationOf(1000, killNode1);
                terminateAfterTerminationOf(5000, killNode2);
            }
        };

        return testEPFD;
    }


    /**
     * Simulation Scenario to test eventual leader detector properties
     * 1. ELD1: eventual completeness: Eventually every correct node trusts some correct node.
     * 2. ELD2: eventual agreement: Eventually no two correct nodes trust different correct node.
     */
    public static SimulationScenario testELD_Properties() {
        SimulationScenario testLeaderElection = new SimulationScenario() {
            {
                StochasticProcess srvNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(9, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };

                StochasticProcess killNode5 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(5));
                    }
                };

                StochasticProcess restartNode5 = new StochasticProcess() {
                    {
                        raise(1, startServerOp, new BasicIntSequentialDistribution(5));
                    }
                };

                StochasticProcess killNode2 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(2));
                    }
                };

                StochasticProcess restartNode2 = new StochasticProcess() {
                    {
                        raise(1, startServerOp, new BasicIntSequentialDistribution(2));
                    }
                };

                StochasticProcess killNode7 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(7));
                    }
                };

                StochasticProcess restartNode7 = new StochasticProcess() {
                    {
                        raise(1, startServerOp, new BasicIntSequentialDistribution(7));
                    }
                };

                srvNodes.start();
                killNode5.startAfterTerminationOf(1000, srvNodes);
                killNode2.startAfterTerminationOf(1000, killNode5);
                killNode7.startAfterTerminationOf(1000, killNode2);
                restartNode7.startAfterTerminationOf(1000, killNode7);
                restartNode2.startAfterTerminationOf(1000, restartNode7);
                restartNode5.startAfterTerminationOf(1000, restartNode2);
                terminateAfterTerminationOf(1000, restartNode5);
            }
        };

        return testLeaderElection;
    }


    /**
     * There are 5 scenarios to test key-value store. They can be summarized as the following:
     *  - testGetEmptyStore: The store is empty, so the getting some unavailable value must return nothing (not found)
     *  - testPutGet: A key-value pair is inserted to the store. We test getting this value by it's key
     *  - testPut: Same as previous operation, without getting any value [testing put operation alone]
     *  - testCasEmptyStore: The store is empty, so the comparing and swapping a value must not go through
     *  - testPutCas: A key-value pair is inserted to the store. We test swapping a key with it's corresponding correct refvalue to a new value
     *  - testPutCasIncorrectRefValue: A key-value pair is inserted to the store. We test swapping a key with it's corresponding incorrect refvalue to a new value
     */

    /**
     * Simulation Scenario to test Get Operation when there are no values in the key-store
     * The Simulation initializes the server with 5 nodes and 3 clients
     */
    public static SimulationScenario testGetEmptyStore() {
        SimulationScenario testGet = new SimulationScenario() {
            {
                StochasticProcess initSrvNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess initClientNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(3, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess getProcess = new StochasticProcess() {
                    {
                        raise(1, getOp, new BasicIntSequentialDistribution(1));
                    }
                };
                initSrvNodes.start();
                initClientNodes.startAfterTerminationOf(5000, initSrvNodes);
                getProcess.startAfterTerminationOf(1000, initClientNodes);
                terminateAfterTerminationOf(7000, getProcess);
            }
        };
        return testGet;
    }

    /**
     * Simulation Scenario to test Put and Get Operations.
     * The Simulation initializes the server with 5 nodes and 3 clients
     */
    public static SimulationScenario testPutGet() {
        SimulationScenario testPutGet = new SimulationScenario() {
            {
                StochasticProcess initSrvNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess initClientNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(3, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess putProcess = new StochasticProcess() {
                    {
                        raise(1, putOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess getProcess = new StochasticProcess() {
                    {
                        raise(1, getOp, new BasicIntSequentialDistribution(1));
                    }
                };

                initSrvNodes.start();
                initClientNodes.startAfterTerminationOf(5000, initSrvNodes);
                putProcess.startAfterTerminationOf(5000, initClientNodes);
                getProcess.startAfterTerminationOf(5000, getProcess);
                terminateAfterTerminationOf(7000, getProcess);
            }
        };
        return testPutGet;
    }

    /**
     * Simulation Scenario to test Put Operation.
     * The Simulation initializes the server with 5 nodes and 3 clients
     */
    public static SimulationScenario testPut() {
        SimulationScenario testPut = new SimulationScenario() {
            {
                StochasticProcess initSrvNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess initClientNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(3, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess putProcess = new StochasticProcess() {
                    {
                        raise(1, putOp, new BasicIntSequentialDistribution(1));
                    }
                };

                initSrvNodes.start();
                initClientNodes.startAfterTerminationOf(5000, initSrvNodes);
                putProcess.startAfterTerminationOf(5000, initClientNodes);
                terminateAfterTerminationOf(1000, putProcess);
            }
        };
        return testPut;
    }


    /**
     * Simulation Scenario to test Cas Operation when there are no keys or values in the key-store
     * The Simulation initializes the server with 5 nodes and 3 clients
     */
    public static SimulationScenario testCasEmptyStore() {
        SimulationScenario testCas = new SimulationScenario() {
            {
                StochasticProcess initSrvNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess initClientNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(3, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess casProcess = new StochasticProcess() {
                    {
                        raise(1, casOp, new BasicIntSequentialDistribution(1), constant(0));
                    }
                };
                initSrvNodes.start();
                initClientNodes.startAfterTerminationOf(5000, initSrvNodes);
                casProcess.startAfterTerminationOf(7000, initClientNodes);
                terminateAfterTerminationOf(9000, casProcess);
            }
        };
        return testCas;
    }

    /**
     * Simulation Scenario to test Cas Operation when there are no keys or values in the key-store
     * The cas will check the correct reference value and swap it with a new value
     * The Simulation initializes the server with 5 nodes and 3 clients
     */
    public static SimulationScenario testPutCas() {
        SimulationScenario testPutCas = new SimulationScenario() {
            {
                StochasticProcess initSrvNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess initClientNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(3, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess putProcess = new StochasticProcess() {
                    {
                        raise(1, putOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess casProcess = new StochasticProcess() {
                    {
                        raise(1, casOp, new BasicIntSequentialDistribution(1), constant(0));
                    }
                };
                initSrvNodes.start();
                initClientNodes.startAfterTerminationOf(5000, initSrvNodes);
                putProcess.startAfterTerminationOf(7000, initClientNodes);
                casProcess.startAfterTerminationOf(7000, putProcess);
                terminateAfterTerminationOf(9500, casProcess);
            }
        };
        return testPutCas;
    }


    /**
     * Simulation Scenario to test Cas Operation when there are no keys or values in the key-store
     * The cas will try to preform with correct key and incorrect reference value
     * The Simulation initializes the server with 5 nodes and 3 clients
     */
    public static SimulationScenario testPutCasIncorrectRefValue() {
        SimulationScenario testPutCasIncorrectRefValue = new SimulationScenario() {
            {
                StochasticProcess initSrvNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, startServerOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess initClientNodes = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(3, startClientOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess putProcess = new StochasticProcess() {
                    {
                        raise(1, putOp, new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess casProcess = new StochasticProcess() {
                    {
                        raise(1, casOp, new BasicIntSequentialDistribution(1), constant(-1));
                    }
                };
                initSrvNodes.start();
                initClientNodes.startAfterTerminationOf(5000, initSrvNodes);
                putProcess.startAfterTerminationOf(7000, initClientNodes);
                casProcess.startAfterTerminationOf(7000, putProcess);
                terminateAfterTerminationOf(9100, casProcess);
            }
        };
        return testPutCasIncorrectRefValue;
    }
}