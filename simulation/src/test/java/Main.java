import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.config.Conversions;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;
import stormy.converters.TAddressConverter;

/**
 * Created by khaled on 2/23/17.
 */
public class Main {

    static final TAddressConverter CONVERTER = new TAddressConverter();
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
