package io.github.ih0rd.adapter.api;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.executors.BaseExecutor;
import io.github.ih0rd.adapter.api.executors.PyExecutor;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High-level adapter for executing polyglot code. Provides unified API for different executors
 * (Python, JS, etc.).
 */
public record PolyglotAdapter(BaseExecutor executor) implements AutoCloseable {

  /** Create adapter for Python with default context. */
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

  /** Generic adapter factory for any executor (future-proof). */
  public static PolyglotAdapter of(BaseExecutor executor) {
    return new PolyglotAdapter(executor);
  }

  /** Evaluate method with arguments. */
  public <T>EvalResult<?> evaluate(
      String methodName, Class<T> memberTargetType, Object... args) {
    return executor.evaluate(methodName, memberTargetType, args);
  }

    /** Evaluate method with native code. */
    public <T>EvalResult<?> evaluate(String code) {
        return executor.evaluate(code);
    }

    /** Evaluate method without arguments. */
  public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType) {
    return executor.evaluate(methodName, memberTargetType);
  }

    public CompletableFuture<EvalResult<?>> evaluateAsync(String code) {
        return executor.evaluateAsync(code);
    }

    public <T> CompletableFuture<EvalResult<?>> evaluateAsync(String methodName, Class<T> memberTargetType, Object... args) {
        return executor.evaluateAsync(methodName, memberTargetType, args);
    }

    public <T> CompletableFuture<EvalResult<?>> evaluateAsync(String methodName, Class<T> memberTargetType) {
        return executor.evaluateAsync(methodName, memberTargetType);
    }


    @Override
  public void close() {
    executor.close();
  }
}
