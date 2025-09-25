package io.github.ih0r.adapter.api.executors;

import io.github.ih0r.adapter.api.context.PolyglotContextFactory;
import io.github.ih0r.adapter.exceptions.EvaluationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PyExecutorCreationTest {

    interface MyApi { void m(); }

    @Test
    void createDefault_andClose() {
        try (PyExecutor exec = PyExecutor.createDefault()) {
            assertNotNull(exec);
        }
    }

    @Test
    void createWithBuilder_andEvaluateMissingFile_throws() {
        PolyglotContextFactory.Builder b = new PolyglotContextFactory.Builder();
        try (PyExecutor exec = PyExecutor.create(b)) {
            EvaluationException ex = assertThrows(EvaluationException.class,
                    () -> exec.evaluate("m", MyApi.class));
            assertTrue(ex.getMessage().contains("Cannot find Python file"));
        }
    }
}
