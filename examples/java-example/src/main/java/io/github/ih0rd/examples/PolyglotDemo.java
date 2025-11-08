package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.executors.JsExecutor;
import io.github.ih0rd.adapter.api.executors.PyExecutor;
import io.github.ih0rd.examples.contracts.ForecastService;
import io.github.ih0rd.examples.contracts.LibrariesApi;
import io.github.ih0rd.examples.contracts.SimplexSolver;
import org.graalvm.polyglot.Value;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PolyglotDemo {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PY_RESOURCES = "/examples/java-example/src/main/resources/python";

    private static final List<Double> Y = List.of(10.0, 12.0, 15.0, 14.0, 18.0, 20.0);
    private static final int STEPS = 4;
    private static final int SEASON_PERIOD = 3;

    public static void main(String[] args) {
        IO.println("🟢 [START] PolyglotExecutor Demo (GraalPy & GraalJS 25.x)\n");

        try {
            runCustomContextExample();
            runSimplexExample();
            runInlinePython();
            runNumpyForecast();
            runJsExample();
            // runAsyncExample();
        } catch (Exception e) {
            IO.println("❌ [FATAL] " + e.getMessage());
            e.printStackTrace();
        }

        IO.println("\n✅ [DONE] All examples executed successfully.");
    }

    // === [1] Custom Context Example ===
    private static void runCustomContextExample() {
        IO.println("\n=== [1] Custom Python Context Example ===");
        var builder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(builder)) {
            LibrariesApi api = executor.bind(LibrariesApi.class);

            var users = api.genUsers(5);
            IO.println("genUsers → " + users);

            var formatted = api.formatUsers(3);
            IO.println("formatUsers → \n" + formatted);

            var paragraphs = api.fakeParagraphs(2);
            IO.println("fakeParagraphs → \n" + paragraphs);
        }
    }

    // === [2] Simplex Solver ===
    private static void runSimplexExample() {
        IO.println("\n=== [2] Simplex Solver Example ===");
        var builder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(builder)) {
            SimplexSolver solver = executor.bind(SimplexSolver.class);

            var A = List.of(List.of(1, 2), List.of(4, 0), List.of(0, 4));
            var B = List.of(8, 16, 12);
            var C = List.of(3, 2);

            var result = solver.runSimplex(A, B, C, "max", null, false, true);
            IO.println("runSimplex → " + result);
        }
    }

    // === [3] Inline Python Evaluation ===
    private static void runInlinePython() {
        IO.println("\n=== [3] Inline Python Code ===");
        var builder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(builder)) {
            Value result = executor.evaluate("sum([i * i for i in range(5)])").as(Value.class);
            IO.println("Python inline result → " + result.asInt());
        }
    }

    // === [4] NumPy Forecast ===
    private static void runNumpyForecast() {
        IO.println("\n=== [4] NumPy Forecast Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (var executor = PyExecutor.createDefault()) {
            ForecastService service = executor.bind(ForecastService.class);
            var forecast = service.forecast(Y, STEPS, SEASON_PERIOD);
            IO.println("forecast → " + forecast);
        }
    }

    // === [5] JavaScript Example ===
    private static void runJsExample() {
        IO.println("\n=== [5] JavaScript Example ===");

        String jsCode = """
                function stats(arr) {
                    const sum = arr.reduce((a, b) => a + b, 0);
                    const avg = sum / arr.length;
                    return { sum, average: avg };
                }
                const data = [2, 4, 6, 8, 10];
                stats(data);
                """;

        var builder = languageBuilder(Language.JS);

        try (var executor = JsExecutor.create(builder)) {
            Value result = executor.evaluate(jsCode).as(Value.class);
            IO.println("JS result → " + result);
        }
    }

    // === [6] Async Example ===
    private static void runAsyncExample() {
        IO.println("\n=== [6] Async Python Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (var executor = PyExecutor.createDefault()) {
            ForecastService service = executor.bind(ForecastService.class);

            IO.println("→ Submitting async forecast...");
            CompletableFuture<?> task = executor.async(() -> service.forecast(Y, STEPS, SEASON_PERIOD));

            IO.println("→ Doing other work...");
            var result = task.join();
            IO.println("Async forecast → " + result);
        }
    }

    // === Helper ===
    private static PolyglotContextFactory.Builder languageBuilder(Language lang) {
        return new PolyglotContextFactory.Builder(lang)
                .withSafePythonDefaults()
                .allowAllAccess(true)
                .allowExperimentalOptions(true);
    }
}
