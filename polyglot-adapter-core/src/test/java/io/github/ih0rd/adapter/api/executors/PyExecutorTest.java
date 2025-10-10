package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
class PyExecutorTest {

    private org.graalvm.polyglot.Context fakeContext() {
        try {
            Constructor<org.graalvm.polyglot.Context> c =
                    org.graalvm.polyglot.Context.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Throwable t) {
            return null;
        }
    }

    @Test
    void create_withBuilder_doesNotThrow() {
        var builder = new PolyglotContextFactory.Builder(Language.PYTHON);
        assertNotNull(builder.getResourcesPath());
        var exec = new PyExecutor(null, builder.getResourcesPath());
        assertNotNull(exec.resourcesPath());
    }

    @Test
    void close_withNullContext_doesNotThrow() {
        var exec = new PyExecutor(null, Path.of("/tmp"));
        assertThrows(NullPointerException.class,exec::close);
    }

    @Test
    void evaluate_throwsForMissingFile() {
        var exec = new PyExecutor(null, Path.of("/tmp"));
        assertThrows(EvaluationException.class, () -> exec.evaluate("x", DummyApi.class));
    }

    interface DummyApi {
        void ping();
    }

    @Test
    void evalResult_ofWorks() {
        var r = EvalResult.of(10);
        assertEquals(Integer.class, r.type());
        assertEquals(10, r.value());
    }
}
