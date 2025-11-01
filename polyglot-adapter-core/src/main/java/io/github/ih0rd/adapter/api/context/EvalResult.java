package io.github.ih0rd.adapter.api.context;

import io.github.ih0rd.adapter.utils.ValueUnwrapper;

/// # EvalResult
/// Immutable record representing a strongly-typed result of a polyglot evaluation.
///
/// ---
/// ### Description
/// Wraps both the **runtime type** and **evaluated value** of a GraalVM expression.
/// Supports safe conversion of polyglot `Value` objects via {@link ValueUnwrapper}.
///
/// ---
/// ### Example
/// ```java
/// EvalResult<?> result = executor.evaluate("sum([1, 2, 3])");
/// int sum = result.as(Integer.class);
/// System.out.println(sum); // → 6
/// ```
public record EvalResult<T>(Class<T> type, T value) {

    /// ### of(value)
    /// Static factory that infers the result type automatically from the provided value.
    ///
    /// ---
    /// #### Example
    /// ```java
    /// EvalResult<?> res = EvalResult.of(42);
    /// System.out.println(res.type()); // Integer
    /// ```
    @SuppressWarnings("unchecked")
    public static <T> EvalResult<T> of(T value) {
        return new EvalResult<>(
                value != null ? (Class<T>) value.getClass() : (Class<T>) Object.class,
                value
        );
    }

    /// ### as(targetType)
    /// Converts the contained value into the specified Java type.
    ///
    /// ---
    /// #### Behavior
    /// - Delegates unwrapping logic to {@link ValueUnwrapper}.
    /// - Handles both pure Java and polyglot {@link org.graalvm.polyglot.Value} instances.
    ///
    /// ---
    /// #### Example
    /// ```java
    /// int sum = result.as(Integer.class);
    /// Map<String, Object> data = result.as(Map.class);
    /// ```
    @SuppressWarnings("unchecked")
    public <R> R as(Class<R> targetType) {
        if (value == null) return null;

        if (value instanceof org.graalvm.polyglot.Value val) {
            return ValueUnwrapper.unwrap(val, targetType);
        }

        if (!targetType.isAssignableFrom(type)) {
            throw new ClassCastException(
                    "Cannot cast " + type.getName() + " to " + targetType.getName()
            );
        }

        return (R) value;
    }

    /// ### toString
    /// Returns a concise string representation of the evaluation result.
    @Override
    public String toString() {
        return "EvalResult[type=%s, value=%s]".formatted(
                type != null ? type.getSimpleName() : "null",
                value
        );
    }
}
