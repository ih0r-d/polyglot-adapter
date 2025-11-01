package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.executors.JsExecutor;
import io.github.ih0rd.adapter.api.executors.PyExecutor;
import io.github.ih0rd.examples.contracts.ForecastService;
import io.github.ih0rd.examples.contracts.LibrariesApi;
import io.github.ih0rd.examples.contracts.SimplexSolver;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PolyglotDemo {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PY_RESOURCES = "/examples/java-example/src/main/resources/python";


    private static final List<Double> Y = List.of(10.0, 12.0, 15.0, 14.0, 18.0, 20.0);
    private static final Integer STEPS = 4;
    private static final Integer SEASON_PERIOND = 3;

    void main() {
        IO.println("🟢 [START] PolyglotExecutor Demo (GraalPy & GraalJS 25.x)\n");

        try {
            runCustomContextExampleNew();
            runSimplexExample();
            runPurePythonCode();
            runNumpyForecast();
            runJsCode();
//            runAsyncExample();
        } catch (Exception e) {
            IO.println("❌ [FATAL] Unexpected error in main: " + e.getMessage());
        }

        IO.println("\n✅ [DONE] All examples executed successfully.");
    }

    // === [3.1] New Custom Context Example ===
    private static void runCustomContextExampleNew() {
        IO.println("\n=== [3] Custom Python Context Example ===");
        var ctxBuilder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(ctxBuilder)) {
            LibrariesApi api = executor.bind(LibrariesApi.class);

            var genUsers = api.genUsers(10);
            IO.println("genUsers → " + genUsers);

            var formatUsers = api.formatUsers(10);
            IO.println("formatUsers → " + formatUsers);

            var fakeParagraphs = api.fakeParagraphs(10);
            IO.println("fakeParagraphs → " + fakeParagraphs);
        }
    }

    // === [4] Simplex Solver Example ===
    private static void runSimplexExample() {
        IO.println("\n=== [4] Simplex Solver Example ===");
        var ctxBuilder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(ctxBuilder)) {
            SimplexSolver solver = executor.bind(SimplexSolver.class);

            var aInput = List.of(List.of(1, 2), List.of(4, 0), List.of(0, 4));
            var bInput = List.of(8, 16, 12);
            var cInput = List.of(3, 2);
            var prob = "max";

            var result = solver.runSimplex(aInput, bInput, cInput, prob, null, false, true);
            IO.println("runSimplex → " + result);
        }
    }

    // === [5] Pure Python Code ===
    private static void runPurePythonCode() {
        IO.println("\n=== [5] Pure Python Inline Evaluation ===");
        var ctxBuilder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(ctxBuilder)) {
            EvalResult<?> result = executor.evaluate("sum([i * i for i in range(5)])");
            IO.println("Python result → " + result);
            IO.println("sum = " + result.as(Double.class));
        }
    }

    // === [6] NumPy Forecast Example ===
    private static void runNumpyForecast() {
        IO.println("\n=== [6] NumPy Forecast Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (var executor = PyExecutor.createDefault()) {
            ForecastService service = executor.bind(ForecastService.class);
            var forecast = service.forecast(Y, STEPS, SEASON_PERIOND);
            IO.println("forecast → " + forecast);
        }
    }

    // === [7] JavaScript Example ===
    private static void runJsCode() {
        IO.println("\n=== [7] JavaScript Example ===");

        String jsCode = """
                function calculateStats(arr) {
                    const sum = arr.reduce((a, b) => a + b, 0);
                    const avg = sum / arr.length;
                    return { sum, average: avg };
                }
                const numbers = [2, 4, 6, 8, 10];
                calculateStats(numbers);
                """;

        var jsBuilder = languageBuilder(Language.JS);

        try (var executor = JsExecutor.create(jsBuilder)) {
            EvalResult<?> result = executor.evaluate(jsCode);
            IO.println("JS result → " + result);
        }
    }

    // === [8] Async Example ===
    private static void runAsyncExample() {
        IO.println("\n=== [8] Async Python Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (var executor = PyExecutor.createDefault()) {
            ForecastService service = executor.bind(ForecastService.class);

            IO.println("→ Submitting async task...");
            CompletableFuture<?> forecast = executor.async(() -> service.forecast(Y, STEPS, SEASON_PERIOND));

            IO.println("→ Doing other work while Python runs...");
            var result = forecast.join();
            IO.println("Async Python forecast → " + result);
        }
    }

    // === Helpers ===
    private static PolyglotContextFactory.Builder languageBuilder(Language lang) {
        return new PolyglotContextFactory.Builder(lang)
                .allowExperimentalOptions(true)
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .withSafePythonDefaults();
    }

}
