package sim.core;

import se.sics.kompics.config.Conversions;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;
import stormy.converters.NetAddressConverter;

/**
 * Created by khaled on 2/25/17.
 */
public class Main {
    static final NetAddressConverter CONVERTER = new NetAddressConverter();
    private static final int NUM_MESSAGES = 10;

    static {
        // conversions
        Conversions.register(CONVERTER);
    }

    private static final SimulationResultMap res = SimulationResultSingleton.getInstance();

    public static void main(String... args) {

        long seed = 123;
        SimulationScenario.setSeed(seed);

        SimulationScenario testEPFD = ScenarioGen.testEPFD_Properties();
        SimulationScenario testELD = ScenarioGen.testELD_Properties();
        SimulationScenario testGetEmptyStore = ScenarioGen.testGetEmptyStore();
        SimulationScenario testPutGet = ScenarioGen.testPutGet();
        SimulationScenario testPut = ScenarioGen.testPut();
        SimulationScenario testCasEmptyStore = ScenarioGen.testCasEmptyStore();
        SimulationScenario testPutCas = ScenarioGen.testPutCas();
        SimulationScenario testPutCasIncorrectRefValue = ScenarioGen.testPutCasIncorrectRefValue();
        SimulationScenario testAbortableSequenceConsensus = ScenarioGen.testAbortableSequenceConsensus();
        testGetEmptyStore.simulate(LauncherComp.class);
    }
}
