package io.github.ih0rd.adapter.api;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.executors.BaseExecutor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PolyglotAdapterTest {

    static class FakeExecutor implements BaseExecutor {
        boolean closed;
        String last;
        @Override
        public <T> EvalResult<?> evaluate(String m, Class<T> c, Object... a) {
            last = m; return EvalResult.of("ok");
        }
        @Override
        public <T> EvalResult<?> evaluate(String m, Class<T> c) {
            last = m; return EvalResult.of("ok");
        }
        @Override
        public void close() { closed = true; }
    }

    interface MyApi { void run(); }

    @Test
    void delegatesEvaluateAndClose() {
        var exec = new FakeExecutor();
        try (var adapter = PolyglotAdapter.of(exec)) {
            var r1 = adapter.evaluate("x", MyApi.class);
            assertEquals(String.class, r1.type());
            assertEquals("ok", r1.value());
        }
        assertTrue(exec.closed);
    }
}
