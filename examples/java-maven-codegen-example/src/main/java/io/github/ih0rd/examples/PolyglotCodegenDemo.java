package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.context.PolyglotHelper;
import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.adapter.spi.ClasspathScriptSource;
import io.github.ih0rd.contract.ScriptSource;
import io.github.ih0rd.contract.SupportedLanguage;
import io.github.ih0rd.examples.polyglot.StatsApi;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Context;

public final class PolyglotCodegenDemo {

    private static final int SEPARATOR_REPEAT = 10;

    public static void main(String[] args) {
        new PolyglotCodegenDemo().run();
    }

    void run() {
        IO.println("=== Polyglot Adapter Demo (Classpath) ===");

        var scriptSource = new ClasspathScriptSource();

        step1RunPython(scriptSource);
        printSeparator();


        IO.println("=== Demo Completed ===");
    }

    private void printSeparator() {
        IO.println("=== === ");

    }

    private void step1RunPython(ScriptSource scriptSource) {
        IO.println("[STEP 1] Python – Classpath");

        try (var ctx = PolyglotHelper.newContext(SupportedLanguage.PYTHON);
                var executor = new PyExecutor(ctx, scriptSource)) {

            StatsApi statsApi = executor.bind(StatsApi.class);

            List<Integer> randomNumbers = statsApi.randomNumbers(SEPARATOR_REPEAT);
            IO.println("random numbers -> " + randomNumbers);

            Map<String, Object> statsMap = statsApi.stats(SEPARATOR_REPEAT);
            IO.println("stats -> " + statsMap);

            String formatted = statsApi.formatStats(SEPARATOR_REPEAT);
            IO.println("formatStats -> " + formatted);
        }
    }

}