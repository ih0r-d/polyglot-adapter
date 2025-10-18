package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.PolyglotAdapter;
import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.executors.JsExecutor;
import io.github.ih0rd.adapter.api.executors.PyExecutor;
import io.github.ih0rd.examples.contracts.ForecastService;
import io.github.ih0rd.examples.contracts.LibrariesApi;
import io.github.ih0rd.examples.contracts.MyApi;
import io.github.ih0rd.examples.contracts.SimplexSolver;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * PolyglotAdapter demonstration (GraalPy & GraalJS 25.x)
 *
 * <p><b>Summary:</b> Demonstrates Java ↔ GraalVM Polyglot integration
 * with Python (GraalPy) and JavaScript (GraalJS).
 *
 * <p><b>Cases:</b>
 * <ol>
 *     <li>Default Python resources</li>
 *     <li>Custom resource directories</li>
 *     <li>Custom context configuration</li>
 *     <li>Simplex solver integration</li>
 *     <li>Pure Python execution (no NumPy)</li>
 *     <li>NumPy forecast execution</li>
 *     <li>Inline JavaScript execution</li>
 *     <li>Async Python execution</li>
 * </ol>
 *
 * <p><b>Notes:</b> On macOS ARM (M1–M4), GraalPy has limited support for native C extensions (e.g. NumPy).
 */
public class PolyglotDemo {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PY_RESOURCES = "/examples/java-example/src/main/resources/python";

    void main() {
        IO.println("🟢 [START] PolyglotAdapter Demo (GraalPy & GraalJS 25.x)\n");

        try {
            runDefaultExample();
            runCustomResourcesExample();
            runCustomContextExample();
            runSimplexExample();
            runPurePythonCode();
//            runNumpyForecast();
            runJsCode();
            runAsyncExample();
        } catch (Exception e) {
            IO.println("❌ [FATAL] Unexpected error in main: " + e.getMessage());
        }

        IO.println("\n✅ [DONE] All examples executed successfully.");
    }

    // === [1] Default Python resources ===
    private static void runDefaultExample() {
        IO.println("\n=== [1] Default Resources Example ===");
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAdd(adapter);
            evaluatePing(adapter);
        } catch (Exception e) {
            IO.println("❌ Error in [runDefaultExample]: " + e.getMessage());
        }
    }

    // === [2] Custom Python resource directory ===
    private static void runCustomResourcesExample() {
        IO.println("\n=== [2] Custom Resources Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAdd(adapter);
            evaluatePing(adapter);
        } catch (Exception e) {
            IO.println("❌ Error in [runCustomResourcesExample]: " + e.getMessage());
        }
    }

    // === [3] Custom Context Builder example ===
    private static void runCustomContextExample() {
        IO.println("\n=== [3] Custom Context Example ===");
        var ctxBuilder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(ctxBuilder);
             var adapter = PolyglotAdapter.of(executor)) {

            var genUsers = adapter.evaluate("genUsers", LibrariesApi.class, 3);
            IO.println("genUsers → " + genUsers);

            var formatUsers = adapter.evaluate("formatUsers", LibrariesApi.class, 3);
            IO.println("formatUsers → " + formatUsers);

            var fakeParagraphs = adapter.evaluate("fakeParagraphs", LibrariesApi.class, 3);
            IO.println("fakeParagraphs → " + fakeParagraphs);

        } catch (Exception e) {
            IO.println("❌ Error in [runCustomContextExample]: " + e.getMessage());
        }
    }

    // === [4] Simplex Solver Example ===
    private static void runSimplexExample() {
        IO.println("\n=== [4] Simplex Service Example ===");
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {

            var aInput = List.of(List.of(1, 2), List.of(4, 0), List.of(0, 4));
            var bInput = List.of(8, 16, 12);
            var cInput = List.of(3, 2);
            var prob = "max";

            var result = adapter.evaluate(
                    "runSimplex",
                    SimplexSolver.class,
                    aInput, bInput, cInput, prob, null, true, true
            );

            IO.println("runSimplex → " + result);

        } catch (Exception e) {
            IO.println("❌ Error in [runSimplexExample]: " + e.getMessage());
        }
    }

    // === [5] Run pure Python (no NumPy) ===
    private static void runPurePythonCode() {
        IO.println("\n=== [5] Run pure Python (no NumPy) ===");
        var ctxBuilder = languageBuilder(Language.PYTHON);

        try (var executor = PyExecutor.create(ctxBuilder);
             var adapter = PolyglotAdapter.of(executor)) {

            EvalResult<?> result = adapter.evaluate("sum([i * i for i in range(5)])");
            IO.println("Python result → " + result);
            IO.println("sum = " + result.as(Double.class));

        } catch (Exception e) {
            IO.println("❌ Error in [runPurePythonCode]: " + e.getMessage());
        }
    }

    // === [6] NumPy Forecast Example ===
    private static void runNumpyForecast() {
        IO.println("\n=== [6] NumPy Forecast Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            var args = numPyArgs();
            EvalResult<?> forecast = adapter.evaluate("forecast", ForecastService.class, args);
            IO.println("forecast → " + forecast);
        } catch (Exception e) {
            IO.println("❌ Error in [runNumpyForecast]: " + e.getMessage());
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
        try (var executor = JsExecutor.create(jsBuilder);
             var adapter = PolyglotAdapter.of(executor)) {

            EvalResult<?> result = adapter.evaluate(jsCode);
            IO.println("JS result → " + result);

        } catch (Exception e) {
            IO.println("❌ Error in [runJsCode]: " + e.getMessage());
        }
    }

    // === [8] Async Python Example ===
    private static void runAsyncExample() {
        IO.println("\n=== [8] Async Python Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {

            IO.println("→ Submitting async task...");

            var args = numPyArgs();
            CompletableFuture<EvalResult<?>> forecast = adapter.evaluateAsync("forecast", ForecastService.class, args);

            IO.println("→ Doing other work while Python runs...");
//            Thread.sleep(10000);
            var result = forecast.get();
            IO.println("Async Python forecast → " + result);

        } catch (Exception e) {
            IO.println("❌ Error in [runAsyncExample]: " + e.getMessage());
        }
    }


    // === Shared helpers ===
    private static PolyglotContextFactory.Builder languageBuilder(Language lang) {
        return new PolyglotContextFactory.Builder(lang)
                .allowExperimentalOptions(true)
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .withSafePythonDefaults();
    }

    private static Object[] numPyArgs(){
        List<Double> y = List.of(10.0, 12.0, 15.0, 14.0, 18.0, 20.0);
        int steps = 4;
        int seasonPeriod = 3;
        return new Object[]{y, steps, seasonPeriod};
    }

    private static void evaluateAdd(PolyglotAdapter adapter) {
        try {
            EvalResult<?> result = adapter.evaluate("add", MyApi.class, 10, 20);
            IO.println("Result(add) → " + result);
        } catch (Exception e) {
            IO.println("❌ Error in [evaluateAdd]: " + e.getMessage());
        }
    }

    private static void evaluatePing(PolyglotAdapter adapter) {
        try {
            EvalResult<?> result = adapter.evaluate("ping", MyApi.class);
            IO.println("Result(ping) → " + result);
        } catch (Exception e) {
            IO.println("❌ Error in [evaluatePing]: " + e.getMessage());
        }
    }
}
