package io.github.ih0r.adapter.api;


import io.github.ih0r.adapter.api.context.PolyglotContextFactory;
import io.github.ih0r.adapter.api.executors.BaseExecutor;
import io.github.ih0r.adapter.api.executors.PyExecutor;

import java.util.Map;

/**
 * High-level adapter for executing polyglot code.
 * Provides unified API for different executors (Python, JS, etc.).
 */
public final class PolyglotAdapter implements AutoCloseable {

    private final BaseExecutor executor;

    private PolyglotAdapter(BaseExecutor executor) {
        this.executor = executor;
    }

    /**
     * Create adapter for Python with default context.
     */
    public static PolyglotAdapter python() {
        return new PolyglotAdapter(PyExecutor.createDefault());
    }

    /**
     * Create adapter for Python with custom context.
     *
     * @param builder PolyglotContextFactory.Builder for fine-tuned setup
     */
    public static PolyglotAdapter python(PolyglotContextFactory.Builder builder) {
        return new PolyglotAdapter(PyExecutor.create(builder));
    }

    /**
     * Generic adapter factory for any executor (future-proof).
     */
    public static PolyglotAdapter of(BaseExecutor executor) {
        return new PolyglotAdapter(executor);
    }

    /**
     * Evaluate method with arguments.
     */
    public <T> Map<String, Object> evaluate(String methodName, Class<T> memberTargetType, Object... args) {
        return executor.evaluate(methodName, memberTargetType, args);
    }

    /**
     * Evaluate method without arguments.
     */
    public <T> Map<String, Object> evaluate(String methodName, Class<T> memberTargetType) {
        return executor.evaluate(methodName, memberTargetType);
    }

    @Override
    public void close() {
        executor.close();
    }
}
