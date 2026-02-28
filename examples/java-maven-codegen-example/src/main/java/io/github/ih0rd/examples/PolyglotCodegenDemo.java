package io.github.ih0rd.examples;

import io.github.ih0rd.adapter.context.PolyglotHelper;
import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.adapter.spi.ClasspathScriptSource;
import io.github.ih0rd.contract.ScriptSource;
import io.github.ih0rd.contract.SupportedLanguage;
import io.github.ih0rd.examples.polyglot.LibrariesApi;
import io.github.ih0rd.examples.polyglot.StatsApi;
import io.github.ih0rd.examples.polyglot.StatsApiV2;

import java.util.List;
import java.util.Map;

public final class PolyglotCodegenDemo {

    private static final int N = 5;

    public static void main(String[] args) {
        new PolyglotCodegenDemo().run();
    }

    void run() {
        IO.println("=== Polyglot Adapter Demo (Classpath) ===");

        var scriptSource = new ClasspathScriptSource();

        runStatsApi(scriptSource);
        printSeparator();

        runStatsApiV2(scriptSource);
        printSeparator();

        runLibrariesApi(scriptSource);
        printSeparator();

        IO.println("=== Demo Completed ===");
    }

    private void printSeparator() {
        IO.println("========================================");
    }

    private void runStatsApi(ScriptSource scriptSource) {
        IO.println("[StatsApi]");

        try (var ctx = PolyglotHelper.newContext(SupportedLanguage.PYTHON);
                var executor = new PyExecutor(ctx, scriptSource)) {

            StatsApi api = executor.bind(StatsApi.class);

            List<Integer> random = api.randomNumbers(N);
            IO.println("randomNumbers -> " + random);

            Map<String, Object> stats = api.stats(N);
            IO.println("stats -> " + stats);

            String formatted = api.formatStats(N);
            IO.println("formatStats -> " + formatted);
        }
    }

    private void runStatsApiV2(ScriptSource scriptSource) {
        IO.println("[StatsApiV2]");

        try (var ctx = PolyglotHelper.newContext(SupportedLanguage.PYTHON);
                var executor = new PyExecutor(ctx, scriptSource)) {

            StatsApiV2 api = executor.bind(StatsApiV2.class);

            List<Integer> random = api.randomNumbers(N);
            IO.println("randomNumbers -> " + random);

            Map<String, Integer> stats = api.stats(N);
            IO.println("stats -> " + stats);

            String formatted = api.formatStats(N);
            IO.println("formatStats -> " + formatted);
        }
    }

    private void runLibrariesApi(ScriptSource scriptSource) {
        IO.println("[LibrariesApi]");

        try (var ctx = PolyglotHelper.newContext(SupportedLanguage.PYTHON);
                var executor = new PyExecutor(ctx, scriptSource)) {

            LibrariesApi api = executor.bind(LibrariesApi.class);

            List<Map<String, Object>> users = api.genUsers(N);
            IO.println("genUsers -> " + users);

            String formatted = api.formatUsers(N);
            IO.println("formatUsers -> " + formatted);

            String paragraphs = api.fakeParagraphs(N);
            IO.println("fakeParagraphs -> " + paragraphs);
        }
    }
}