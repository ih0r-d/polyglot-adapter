package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.PolyglotAdapter;
import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.executors.PyExecutor;
import io.github.ih0rd.examples.contracts.LibrariesApi;
import io.github.ih0rd.examples.contracts.MyApi;
import io.github.ih0rd.examples.contracts.SimplexSolver;

import java.util.List;

/**
 * PolyglotAdapter demonstration (GraalPy 25.x)
 *
 * Showcases:
 *  1. Default resources (src/main/python)
 *  2. Custom resources via System property
 *  3. Custom context configuration
 *  4. SimplexService integration
 */
public class PolyglotDemo {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PY_RESOURCES = "/examples/java-example/src/main/resources/python";

    public static void main(String[] args) {
        IO.println("🟢 [START] PolyglotAdapter Demo (GraalPy 25.x)\n");

        runDefaultExample();
        runCustomResourcesExample();
        runCustomContextExample();
        runSimplexExample();

        IO.println("\n✅ [DONE] All examples executed successfully.");
    }

    /** ───────────────────────────────
     *  CASE 1 — Default Python resources
     *  Loads scripts from src/main/python
     *  ─────────────────────────────── */
    private static void runDefaultExample() {
        IO.println("\n=== [1] Default Resources Example ===");
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAdd(adapter);
            evaluatePing(adapter);
        } catch (Exception e) {
            IO.println("❌ Error in runDefaultExample: " + e.getMessage());
        }
    }

    /** ───────────────────────────────
     *  CASE 2 — Custom resource directory
     *  Overrides py.polyglot-resources.path
     *  ─────────────────────────────── */
    private static void runCustomResourcesExample() {
        IO.println("\n=== [2] Custom Resources Example ===");
        System.setProperty(PY_RESOURCES_KEY, PROJECT_DIR + PY_RESOURCES);

        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAdd(adapter);
            evaluatePing(adapter);
        } catch (Exception e) {
            IO.println("❌ Error in runCustomResourcesExample: " + e.getMessage());
        }
    }

    /** ───────────────────────────────
     *  CASE 3 — Custom context builder
     *  Demonstrates GraalPy context tuning
     *  ─────────────────────────────── */
    private static void runCustomContextExample() {
        IO.println("\n=== [3] Custom Context Example ===");

        var ctxBuilder = new PolyglotContextFactory.Builder(Language.PYTHON)
                .allowExperimentalOptions(true)
                .allowAllAccess(true)
                .allowNativeAccess(true);

        try (var executor = PyExecutor.create(ctxBuilder);
             var adapter = PolyglotAdapter.of(executor)) {

            var genUsers = adapter.evaluate("genUsers", LibrariesApi.class, 3);
            IO.println("genUsers → " + genUsers);

            var formatUsers = adapter.evaluate("formatUsers", LibrariesApi.class, 3);
            IO.println("formatUsers → " + formatUsers);

            var fakeParagraphs = adapter.evaluate("fakeParagraphs", LibrariesApi.class, 3);
            IO.println("fakeParagraphs → " + fakeParagraphs);

        } catch (Exception e) {
            IO.println("❌ Error in runCustomContextExample: " + e.getMessage());
        }
    }

    /** ───────────────────────────────
     *  CASE 4 — Simplex Service Demo
     *  Demonstrates mathematical service call
     *  ─────────────────────────────── */
    private static void runSimplexExample() {
        IO.println("\n=== [4] Simplex Service Example ===");
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {

            var aInput = List.of(List.of(1, 2), List.of(4, 0), List.of(0, 4));
            var bInput = List.of(8, 16, 12);
            var cInput = List.of(3, 2);
            var prob = "max";
            var enableMsg = true;
            var latex = true;

            var result = adapter.evaluate("runSimplex",
                    SimplexSolver.class,
                    aInput, bInput, cInput, prob, null, enableMsg, latex);

            IO.println("runSimplex → " + result);

        } catch (Exception e) {
            IO.println("❌ Error in runSimplexExample: " + e.getMessage());
        }
    }

    /** ───────────────────────────────
     *  HELPER METHODS
     *  ─────────────────────────────── */
    private static void evaluateAdd(PolyglotAdapter adapter) {
        EvalResult<?> result = adapter.evaluate("add", MyApi.class, 10, 20);
        IO.println("Result(add) → " + result);
    }

    private static void evaluatePing(PolyglotAdapter adapter) {
        EvalResult<?> result = adapter.evaluate("ping", MyApi.class);
        IO.println("Result(ping) → " + result);
    }
}
