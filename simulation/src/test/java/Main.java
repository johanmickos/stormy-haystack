import se.sics.kompics.config.Conversions;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;
import sim.core.ScenarioGen;
import sim.core.SimulationResultMap;
import sim.core.SimulationResultSingleton;
import stormy.converters.NetAddressConverter;

/**
 * Created by khaled on 2/23/17.
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
        SimulationScenario simpleBootScenario = ScenarioGen.simpleOps(3);
        res.put("messages", NUM_MESSAGES);
        simpleBootScenario.simulate(LauncherComp.class);

        SimulationScenario killPongersScenario = ScenarioGen.testEPFD_Properties();
        killPongersScenario.simulate(LauncherComp.class);
    }
}
