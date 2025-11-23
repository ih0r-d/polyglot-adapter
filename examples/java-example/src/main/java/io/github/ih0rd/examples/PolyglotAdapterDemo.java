package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.context.JsExecutor;
import io.github.ih0rd.adapter.context.PolyglotHelper;
import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.adapter.context.SupportedLanguage;
import io.github.ih0rd.examples.contracts.BrokenForecastService;
import io.github.ih0rd.examples.contracts.ForecastService;
import io.github.ih0rd.examples.contracts.SimplexSolver;
import io.github.ih0rd.examples.contracts.StatsApi;
import java.util.List;
import java.util.Map;

public class PolyglotAdapterDemo {

    private static final String PY_RESOURCES_PROPERTY = "py.polyglot-resources.path";
    private static final String PROJECT_DIR = System.getProperty("user.dir");

    private static final int SEPARATOR_REPEAT = 10;

    private static final List<Double> DATA = List.of(2.0, 3.5, 4.2, 5.0, 6.1, 8.3);
    private static final int STEPS = 3;
    private static final int PERIOD = 2;

    void main() {
        new PolyglotAdapterDemo().run();
    }

    void run() {
        IO.println("=== Polyglot Adapter Demo (GraalVM 25) ===");

        IO.println("[STEP 1] Python – default context");
        step1RunPythonWithDefaultContext();
        printSeparator();

        IO.println("[STEP 2] Python – custom context");
        step2RunPythonWithContext();
        printSeparator();

        IO.println("[STEP 3] Python – extra resource folder");
        step3RunPythonWithExtraResourceFolder();
        printSeparator();
//
        IO.println("[STEP 4] JavaScript – default context");
        step4RunJsWithContext();
        printSeparator();

        IO.println("[STEP 5] Python – binding validation (OK + broken)");
        step5RunPythonWithBrokenValidation();
        printSeparator();

        IO.println("[STEP 6] JavaScript – binding validation (OK + broken)");
        step6RunJsWithBrokenValidation();

        IO.println("[STEP 7] Executors metadata");
        step7PrintMetadata();

        IO.println("=== Demo Completed ===");
    }

    private void printSeparator() {
        IO.println("=== === ".repeat(SEPARATOR_REPEAT));
    }

    private void step1RunPythonWithDefaultContext() {
        try (PyExecutor executor = PyExecutor.createDefault()) {
            executor.clearAllCaches();
            StatsApi statsApi = executor.bind(StatsApi.class);

            List<Integer> randomNumbers = statsApi.randomNumbers(SEPARATOR_REPEAT);
            IO.println("random numbers -> " + randomNumbers);

            var statsMap = statsApi.stats(SEPARATOR_REPEAT);
            IO.println("stats -> " + statsMap);

            String formatted = statsApi.formatStats(SEPARATOR_REPEAT);
            IO.println("formatStats -> " + formatted);
        }
    }

    private void step2RunPythonWithContext() {
        try (var ctx = PolyglotHelper.newContext(
                SupportedLanguage.PYTHON,
                b -> b.option("engine.WarnInterpreterOnly", "false")
                        .option("python.WarnExperimentalFeatures", "false"));
                var executor = PyExecutor.createWithContext(ctx)) {

            ForecastService service = executor.bind(ForecastService.class);
            var forecastMap = service.forecast(DATA, STEPS, PERIOD);
            IO.println("py forecast -> " + forecastMap);
        }
    }

    private void step3RunPythonWithExtraResourceFolder() {
        String previous = System.getProperty(PY_RESOURCES_PROPERTY);
        System.setProperty(PY_RESOURCES_PROPERTY, PROJECT_DIR + "/python");

        try (var ctx = PolyglotHelper.newContext(
                SupportedLanguage.PYTHON,
                b -> b.option("engine.WarnInterpreterOnly", "false")
                        .option("python.WarnExperimentalFeatures", "false")
                        .option("python.IsolateNativeModules", "true"));
                var executor = PyExecutor.createWithContext(ctx)) {

            SimplexSolver simplexSolver = executor.bind(SimplexSolver.class);

            List<List<Integer>> aInput = List.of(
                    List.of(1, 2, 3),
                    List.of(4, 5, 6)
            );
            List<Integer> bInput = List.of(2, 3, 4);
            List<Integer> cInput = List.of(3, 4, 5);
            String prob = "test";
            String iNeq = "data";
            boolean enableMsg = false;
            boolean latex = false;
            var resultMap = simplexSolver.runSimplex(aInput, bInput, cInput, prob, iNeq, enableMsg, latex);

            IO.println("simplex solver -> " + resultMap);
        } finally {
            if (previous != null) {
                System.setProperty(PY_RESOURCES_PROPERTY, previous);
            } else {
                System.clearProperty(PY_RESOURCES_PROPERTY);
            }
        }
    }

    private void step4RunJsWithContext() {
        try (JsExecutor executor = JsExecutor.createDefault()) {
            executor.validateBinding(ForecastService.class);

            ForecastService service = executor.bind(ForecastService.class);
            Map<String, Object> forecast = service.forecast(DATA, STEPS, PERIOD);
            IO.println("js forecast -> " + forecast);
        }
    }

    private void step5RunPythonWithBrokenValidation() {
        try (PyExecutor py = PyExecutor.createDefault()) {

            try {
                py.validateBinding(ForecastService.class);
                IO.println("[PY] ForecastService binding OK");
            } catch (Exception e) {
                IO.println("[PY] ForecastService validation FAILED (unexpected): " + e.getMessage());
            }

            try {
                py.validateBinding(BrokenForecastService.class);
                IO.println("[PY] BrokenForecastService binding UNEXPECTED OK");
            } catch (Exception e) {
                IO.println("[PY] BrokenForecastService validation FAILED (expected): " + e.getMessage());
            }
        }
    }

    private void step6RunJsWithBrokenValidation() {
        try (JsExecutor js = JsExecutor.createDefault()) {

            try {
                js.validateBinding(ForecastService.class);
                IO.println("[JS] ForecastService binding OK");
            } catch (Exception e) {
                IO.println("[JS] ForecastService validation FAILED (unexpected): " + e.getMessage());
            }

            try {
                js.validateBinding(BrokenForecastService.class);
                IO.println("[JS] BrokenForecastService binding UNEXPECTED OK");
            } catch (Exception e) {
                IO.println("[JS] BrokenForecastService validation FAILED (expected): " + e.getMessage());
            }
        }
    }

    private void step7PrintMetadata() {
        IO.println("[STEP 7] Executor metadata snapshot");

        try (PyExecutor py = PyExecutor.createDefault();
                JsExecutor js = JsExecutor.createDefault()) {

            py.validateBinding(StatsApi.class);
            js.validateBinding(ForecastService.class);

            IO.println("[PY] metadata -> " + py.metadata());
            IO.println("[JS] metadata -> " + js.metadata());
        }
    }

}
