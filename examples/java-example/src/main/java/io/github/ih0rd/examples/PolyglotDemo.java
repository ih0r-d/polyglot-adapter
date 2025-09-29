package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.api.PolyglotAdapter;
import io.github.ih0rd.examples.contracts.LibrariesApi;
import io.github.ih0rd.examples.contracts.MyApi;

import java.util.Map;

import static java.lang.IO.println;

/**
 * Demo app for PolyglotAdapter.
 * Shows two use cases:
 *  1. Default resources (src/main/python)
 *  2. Custom resources path via System property
 */
public class PolyglotDemo {

    private static final String PROJECT_DIR =  System.getProperty("user.dir");
    private static final String PY_RESOURCES_KEY = "py.polyglot-resources.path";
    private static final String PY_RESOURCES = "/examples/java-example/src/main/resources/python";

    void main() {
        println("=== Running with DEFAULT resources ===");
        runWithDefaultResources();

        System.out.println("\n=== Running with CUSTOM resources ===");
        runWithCustomResources();
    }

    /** Case 1: uses default resources path from Constants.DEFAULT_PY_RESOURCES */
    private static void runWithDefaultResources() {
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAddMethod(adapter);
            evaluatePingMethod(adapter);
            evaluateGetUserMethod(adapter,1);
        }
    }

    /** Case 2: override Python resources path with system property */
    private static void runWithCustomResources() {
        String customPath = PROJECT_DIR + PY_RESOURCES;
        System.setProperty(PY_RESOURCES_KEY, customPath);

        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            evaluateAddMethod(adapter);
            evaluatePingMethod(adapter);

            evaluateGetUserMethod(adapter,1);
        }
    }

    private static void evaluateAddMethod(PolyglotAdapter adapter) {
        Map<String, Object> addMap = adapter.evaluate("add", MyApi.class, 10, 20);
        println("Result (add): " + addMap);
    }

    private static void evaluatePingMethod(PolyglotAdapter adapter) {
        Map<String, Object> pingMap = adapter.evaluate("ping", MyApi.class);
        println("Result (ping): " + pingMap);
    }

    private static void evaluateGetUserMethod(PolyglotAdapter adapter, int userId) {
        Map<String, Object> getUserMap = adapter.evaluate("get_user", LibrariesApi.class, userId);
        println("Result (get_user): " + getUserMap);
    }
}
