package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.executors.JsExecutor;
import io.github.ih0rd.adapter.api.executors.PyExecutor;
import io.github.ih0rd.examples.contracts.LibrariesApi;
import io.github.ih0rd.examples.contracts.ForecastService;
import org.graalvm.polyglot.Value;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PolyglotDemo {

    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PY_RESOURCES = "/examples/java-example/src/main/resources/python";

    private static final List<Double> DATA = List.of(2.0, 3.5, 4.2, 5.0, 6.1, 8.3);
    private static final int STEPS = 3;
    private static final int PERIOD = 2;

    void main() {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");

        IO.println("=== Polyglot Adapter Demo (GraalVM 25) ===");
        step1CustomPython();
        step2InlinePython();
        step3ForecastSync();
        step4InlineJs();
        IO.println("=== Demo Completed ===");
    }

    /// ### Step 1: Python context with safe defaults and LibrariesApi binding
    private static void step1CustomPython() {
        IO.println("\n[STEP 1] Python Context + LibrariesApi");
        var builder = new PolyglotContextFactory.Builder(Language.PYTHON)
                .withSafePythonDefaults()
                .apply(b -> b.option("python.VerboseFlag", "false"));

        try (var executor = PyExecutor.create(builder)) {
            LibrariesApi api = executor.bind(LibrariesApi.class);
            IO.println("Users → " + api.genUsers(2));
            IO.println("Paragraphs → " + api.fakeParagraphs(1));
        }
    }

    /// ### Step 2: Inline Python evaluation block
    private static void step2InlinePython() {
        IO.println("\n[STEP 2] Inline Python Evaluation");
        var builder = new PolyglotContextFactory.Builder(Language.PYTHON).withSafePythonDefaults();
        try (var executor = PyExecutor.create(builder)) {
            String code = """
                import math
                nums = [3,4,5]
                squares = [n**2 for n in nums]
                {"sqrt81": math.sqrt(81), "sum": sum(squares)}
                """;
            Value result = executor.evaluate(code);
            IO.println("Result → " + result);
        }
    }

    /// ### Step 3: Synchronous forecast using NumPy
    private static void step3ForecastSync() {
        IO.println("\n[STEP 3] Sync ForecastService");
        System.setProperty(PY_RESOURCES_KEY, System.getProperty("user.dir") + PY_RESOURCES);
        try (var executor = PyExecutor.createDefault()) {
            ForecastService service = executor.bind(ForecastService.class);
            var result = service.forecast(DATA, STEPS, PERIOD);
            IO.println("Forecast (sync) → " + result);
        }
    }


    /// ### Step 4: Inline JavaScript evaluation block
    private static void step4InlineJs() {
        IO.println("\n[STEP 4] JavaScript Inline Block");
        var builder = new PolyglotContextFactory.Builder(Language.JS).withNodeSupport();
        try (var executor = JsExecutor.create(builder)) {
            String js = """
                const arr = [1,2,3,4,5];
                const sum = arr.reduce((a,b)=>a+b,0);
                const avg = sum/arr.length;
                ({sum, avg});
                """;
            Value result = executor.evaluate(js);
            IO.println("JS → " + result);
        }
    }
}
