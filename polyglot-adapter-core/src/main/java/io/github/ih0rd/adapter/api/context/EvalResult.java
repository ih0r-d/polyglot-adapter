package io.github.ih0rd.adapter.api.context;

/**
 * Represents a strongly typed result of polyglot evaluation.
 *
 * @param type  the Java type of the evaluated result
 * @param value the evaluated result value
 */
public record EvalResult<T>(Class<T> type, T value) {

    /**
     * Static factory for inferring type automatically from value.
     */
    @SuppressWarnings("unchecked")
    public static <T> EvalResult<T> of(T value) {
        return new EvalResult<>(
                value != null ? (Class<T>) value.getClass() : (Class<T>) Object.class,
                value
        );
    }

    @Override
    public String toString() {
        return "EvalResult[type=%s, value=%s]".formatted(
                type != null ? type.getSimpleName() : "null",
                value
        );
    }
}
