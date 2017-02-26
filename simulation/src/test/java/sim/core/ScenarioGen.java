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

import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation1;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.KillNodeEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;
import sim.wrapper.EldParent;
import sim.wrapper.EpfdParent;
import stormy.ParentComponent;
import stormy.networking.NetAddress;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

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
                    return ScenarioClient.class;
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

    static Operation1 epfdGroupNode = new Operation1<StartNodeEvent, Integer>() {
        @Override
        public StartNodeEvent generate(final Integer self) {
            logger.info("Generating StartEpfdNodeEvent");
            return new StartNodeEvent() {
                NetAddress selfAdr;
                int port = self + 10000;

                {
                    selfAdr = new NetAddress(new InetSocketAddress("192.168.0." + self, 45678));
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return EpfdParent.class;
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public String toString() {
                    return "Start EPFD <" + selfAdr.toString() + ">";
                }
            };
        }
    };


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

    static Operation1 eldGroupNode = new Operation1<StartNodeEvent, Integer>() {
        @Override
        public StartNodeEvent generate(final Integer self) {
            logger.info("Generating StartEpfdNodeEvent");
            return new StartNodeEvent() {
                NetAddress selfAdr;

                {
                    selfAdr = new NetAddress(new InetSocketAddress("192.168.0." + self, 45678));
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return EldParent.class;
                }

                @Override
                public Init getComponentInit() {
                    return Init.NONE;
                }

                @Override
                public String toString() {
                    return "Start ELD <" + selfAdr.toString() + ">";
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

                SimulationScenario.StochasticProcess startEPFD = new SimulationScenario.StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(500));
                        raise(5, epfdGroupNode, new BasicIntSequentialDistribution(1));
                    }
                };

                StochasticProcess killNode1 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(2));
                    }
                };

                StochasticProcess killNode2 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(3));
                    }
                };

                startEPFD.start();
                killNode1.startAfterTerminationOf(5000, startEPFD);
                killNode2.startAfterTerminationOf(1000, killNode1);
                terminateAfterTerminationOf(50000, startEPFD);
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
                StochasticProcess nodeGroupProcess = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(0));
                        raise(5, eldGroupNode, new BasicIntSequentialDistribution(1));
                    }
                };

                //Kill node in even group with ip ending with 2
                StochasticProcess killNode2 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(2));
                    }
                };


                //Restart node in even group with ip ending with 2
                StochasticProcess restartNode2 = new StochasticProcess() {
                    {
                        raise(1, eldGroupNode, new BasicIntSequentialDistribution(2));
                    }
                };

                StochasticProcess killNode1 = new StochasticProcess() {
                    {
                        raise(1, killNode, new BasicIntSequentialDistribution(1));
                    }
                };


                //Restart node in even group with ip ending with 2
                StochasticProcess restartNode1 = new StochasticProcess() {
                    {
                        raise(1, eldGroupNode, new BasicIntSequentialDistribution(1));
                    }
                };

                nodeGroupProcess.start();
                killNode1.startAfterTerminationOf(1000, nodeGroupProcess);
                killNode2.startAfterTerminationOf(1000, killNode1);
                restartNode2.startAfterTerminationOf(1000, killNode2);
                restartNode1.startAfterTerminationOf(1000, restartNode2);
                terminateAfterTerminationOf(1000, restartNode1);
            }
        };

        return testLeaderElection;
    }
}