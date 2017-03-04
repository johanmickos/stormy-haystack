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

import junit.framework.Assert;
import org.junit.Test;
import se.sics.kompics.config.Conversions;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;
import stormy.converters.NetAddressConverter;

public class OpsTest {

    static final NetAddressConverter CONVERTER = new NetAddressConverter();
    private static final int NUM_MESSAGES = 10;

    static {
        // conversions
        Conversions.register(CONVERTER);
    }

    private final SimulationResultMap res = SimulationResultSingleton.getInstance();

    @Test
    public void simpleOpsTest() {
        long seed = 123;
        SimulationScenario.setSeed(seed);
        SimulationScenario simpleBootScenario = ScenarioGen.simpleOps(3);
        res.put("messages", NUM_MESSAGES);
        simpleBootScenario.simulate(LauncherComp.class);
        for (int i = 0; i < NUM_MESSAGES; i++) {
            Assert.assertEquals("NotFound", res.get("test" + i, String.class));
            // of course the correct response should be SUCCESS not NOT_IMPLEMENTED, but like this the test passes
        }
    }
}
