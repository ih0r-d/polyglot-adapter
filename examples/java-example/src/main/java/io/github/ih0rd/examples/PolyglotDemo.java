package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.PolyglotAdapter;
import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.executors.PyExecutor;
import io.github.ih0rd.examples.contracts.LibrariesApi;
import io.github.ih0rd.examples.contracts.MyApi;

import static java.lang.IO.println;

/**
 * Demo app for PolyglotAdapter.
 * Shows two use cases:
 * 1. Default resources (src/main/python)
 * 2. Custom resources path via System property
 * 3. Override context builder with change default values
 */
public class PolyglotDemo {

    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PY_RESOURCES = "/examples/java-example/src/main/resources/python";

    void main() {
//        println("=== Running with DEFAULT resources ===");
//        runWithDefaultResources();
//
//        System.out.println("\n=== Running with CUSTOM resources ===");
//        runWithCustomResources();

        System.out.println("\n=== Running with CUSTOM adapter ===");
        runWithCustomAdapter();
    }

    /**
     * Case 1: uses default resources path from Constants.DEFAULT_PY_RESOURCES
     */
    private static void runWithDefaultResources() {
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAddMethod(adapter);
            evaluatePingMethod(adapter);
        }
    }

    /**
     * Case 2: override Python resources path with system property
     */
    private static void runWithCustomResources() {
        String customPath = PROJECT_DIR + PY_RESOURCES;
        System.setProperty(PY_RESOURCES_KEY, customPath);

        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAddMethod(adapter);
            evaluatePingMethod(adapter);

        }
    }

    /**
     * Case 3: override context builder with change default values
     */
    private static void runWithCustomAdapter(){
        PolyglotContextFactory.Builder ctx = new PolyglotContextFactory.Builder(Language.PYTHON).allowExperimentalOptions(true);
        try (var executor = PyExecutor.create(ctx); var adapter = PolyglotAdapter.of(executor)) {
            var genUsers = adapter.evaluate("genUsers", LibrariesApi.class, 5);
            println("gen_users = " + genUsers);
            println("\n".repeat(3));
            var formatUsers = adapter.evaluate("formatUsers", LibrariesApi.class, 5);
            println("formatUsers = " + formatUsers);
            println("\n".repeat(3));
            var fakeParagraphs = adapter.evaluate("fakeParagraphs", LibrariesApi.class, 5);
            println("fakeParagraphs = " + fakeParagraphs);
        }
    }

    private static void evaluateAddMethod(PolyglotAdapter adapter) {
        var addMap = adapter.evaluate("add", MyApi.class, 10, 20);
        println("Result (add): " + addMap);
    }

    private static void evaluatePingMethod(PolyglotAdapter adapter) {
        var pingMap = adapter.evaluate("ping", MyApi.class);
        println("Result (ping): " + pingMap);
    }

}
