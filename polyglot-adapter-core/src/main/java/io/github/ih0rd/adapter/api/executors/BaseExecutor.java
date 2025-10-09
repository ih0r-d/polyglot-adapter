package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.EvalResult;

import java.util.Map;

/**
 * Base contract for all language executors (Python, JS, R, etc). Not part of the public SDK API.
 */
public interface BaseExecutor extends AutoCloseable {
    <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType, Object... args);

    <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType);

    @Override
    void close();
}
