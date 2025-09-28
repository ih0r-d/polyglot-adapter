package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.PolyglotAdapter;
import java.util.Map;

import static java.lang.IO.println;

/**
 * Demo app for PolyglotAdapter.
 * Shows two use cases:
 *  1. Default resources (src/main/python)
 *  2. Custom resources path via System property
 */
public class PolyglotDemo {

    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";

    void main() {
        println("=== Running with DEFAULT resources ===");
        runWithDefaultResources();

        System.out.println("\n=== Running with CUSTOM resources ===");
        runWithCustomResources();
    }

    /** Case 1: uses default resources path from Constants.DEFAULT_PY_RESOURCES */
    private static void runWithDefaultResources() {
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            Map<String, Object> addMap = adapter.evaluate("add", MyApi.class, 1, 2);
            println("Result (default): " + addMap);
        }
    }

    /** Case 2: override Python resources path with system property */
    private static void runWithCustomResources() {
        String projectRoot = System.getProperty("user.dir");
        String customPath = projectRoot + "/examples/java-example/src/main/resources/python";
        System.setProperty(PY_RESOURCES_KEY, customPath);

        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            Map<String, Object> addMap = adapter.evaluate("add", MyApi.class, 10, 20);
            println("Result (custom path): " + addMap);
        }
    }
}
