package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.context.PolyglotHelper;
import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.adapter.context.SupportedLanguage;
import io.github.ih0rd.examples.contracts.ForecastService;
import io.github.ih0rd.examples.contracts.SimplexSolver;
import io.github.ih0rd.examples.contracts.StatsApi;
import java.util.List;
import java.util.Map;

public class PolyglotAdapterDemo {

    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PROJECT_DIR = System.getProperty("user.dir");

    private static final int COUNT = 10;

    void main() {
        IO.println("=== Polyglot Adapter Demo (GraalVM 25) ===");
        step1PythonWithDefaultContext();
        IO.println("===".repeat(COUNT));
        step2PythonWithContext();
        IO.println("===".repeat(COUNT));
        step3PythonWithExtraResourceFolder();
        IO.println("=== Demo Completed ===");
    }

    void step1PythonWithDefaultContext() {
        try (PyExecutor executor = PyExecutor.createDefault()) {
            StatsApi statsApi = executor.bind(StatsApi.class);
            List<Integer> randomNumbers = statsApi.randomNumbers(COUNT);
            IO.println("random numbers -> " + randomNumbers);
            Map<String, Object> stats = statsApi.stats(COUNT);
            IO.println("stats -> " + stats);
            String formatStats = statsApi.formatStats(COUNT);
            IO.println("formatStats -> " + formatStats);
        }
    }

    void step2PythonWithContext() {
        List<Double> DATA = List.of(2.0, 3.5, 4.2, 5.0, 6.1, 8.3);
        int STEPS = 3;
        int PERIOD = 2;
        try (var ctx = PolyglotHelper.newContext(SupportedLanguage.PYTHON,
                b -> b.option("engine.WarnInterpreterOnly", "false")
                        .option("python.WarnExperimentalFeatures", "false"));
                var executor = PyExecutor.createWithContext(ctx)) {
            ForecastService s = executor.bind(ForecastService.class);
            IO.println(s.forecast(DATA, STEPS, PERIOD));
        }
    }

    void step3PythonWithExtraResourceFolder() {
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + "/python");

        try (var ctx = PolyglotHelper.newContext(SupportedLanguage.PYTHON,
                b -> b.option("engine.WarnInterpreterOnly", "false")
                        .option("python.WarnExperimentalFeatures", "false")
        );
                var executor = PyExecutor.createWithContext(ctx)) {

            SimplexSolver simplexSolver = executor.bind(SimplexSolver.class);
            List<List<Integer>> aInput = List.of(List.of(1,2,3),List.of(4,5,6));
            List<Integer> bInput =  List.of(2,3,4);
            List<Integer> cInput =  List.of(3,4,5);
            String prob = "test";
            String iNeq = "data";
            boolean enableMsg = false;
            boolean latex = false;
            var stringObjectMap = simplexSolver.runSimplex(aInput, bInput, cInput, prob, iNeq, enableMsg, latex);
            IO.println("simplex solver -> " + stringObjectMap);
        }
    }

}
