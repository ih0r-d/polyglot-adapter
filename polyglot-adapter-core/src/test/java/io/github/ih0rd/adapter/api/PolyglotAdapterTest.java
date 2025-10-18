package io.github.ih0rd.adapter.api;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.executors.BaseExecutor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotAdapterTest {

    static class FakeExecutor implements BaseExecutor {
        boolean closed;
        String last;

        @Override
        public <T> EvalResult<?> evaluate(String m, Class<T> c, Object... a) {
            last = m;
            return EvalResult.of("ok");
        }

        @Override
        public <T> EvalResult<?> evaluate(String m, Class<T> c) {
            last = m;
            return EvalResult.of("ok");
        }

        @Override
        public <T> EvalResult<?> evaluate(String code) {
            last = code;
            return EvalResult.of("ok");
        }

        @Override
        public String languageId() {
            return "test";
        }

        @Override
        public org.graalvm.polyglot.Context context() {
            return null; // not needed for this fake
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    interface MyApi {
        void run();
    }

    @Test
    void delegatesEvaluateAndClose() {
        var exec = new FakeExecutor();
        try (var adapter = PolyglotAdapter.of(exec)) {
            var r1 = adapter.evaluate("x", MyApi.class);
            assertEquals("ok", r1.value());
            assertEquals(String.class, r1.type());
        }
        assertTrue(exec.closed);
    }

    @Test
    void evaluateAsync_works() throws Exception {
        try(var exec = new FakeExecutor()){
        CompletableFuture<EvalResult<?>> f = CompletableFuture.supplyAsync(() -> exec.evaluate("code"));
        var result = f.get();
        assertEquals("ok", result.value());
        assertEquals(String.class, result.type());
    }
    }
}
