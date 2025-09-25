package io.github.ih0r.adapter.api.executors;

import java.util.Map;

/**
 * Base contract for all language executors (Python, JS, R, etc).
 * Not part of the public SDK API.
 */
public interface BaseExecutor extends AutoCloseable {

    <T> Map<String, Object> evaluate(String methodName, Class<T> memberTargetType, Object... args);

    <T> Map<String, Object> evaluate(String methodName, Class<T> memberTargetType);

    @Override
    void close();
}