package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Safe tests for {@link PyExecutor}.
 * These tests avoid requiring real GraalPy VFS.
 */
class PyExecutorTest {

    private static PyExecutor executor;

    @BeforeAll
    static void setup() {
        // Mock-like builder that avoids GraalPy VFS
        var builder = new PolyglotContextFactory.Builder(Language.JS); // JS mock backend
        executor = new PyExecutor(Context.newBuilder("js").build(), Path.of("."));
    }

    @AfterAll
    static void cleanup() {
        executor.close();
    }


    @Test
    void async_executesSuccessfully() throws ExecutionException, InterruptedException {
        var future = executor.async(() -> 123);
        assertEquals(123, future.get());
    }

    @Test
    void bind_createsProxyWithoutThrowing() {
        interface DummyApi {
            void ping();
        }
        DummyApi api = executor.bind(DummyApi.class);
        assertNotNull(api);
    }

    @Test
    void evaluate_invalidMethod_throwsException() {
        assertThrows(EvaluationException.class, () ->
                executor.evaluate("invalidMethodName", Object.class, "arg1"));
    }

    @Test
    void close_doesNotThrow() {
        assertDoesNotThrow(executor::close);
    }
}
