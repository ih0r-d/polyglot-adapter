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

    @SuppressWarnings("unchecked")
    public <R> R as(Class<R> targetType) {
        if (value == null) return null;

        if (value instanceof org.graalvm.polyglot.Value val) {
            if (val.isNull()) return null;
            if (targetType == String.class && val.isString()) {
                return (R) val.asString();
            }
            if ((Number.class.isAssignableFrom(targetType) || targetType.isPrimitive()) && val.isNumber()) {
                Number num = val.as(Number.class);
                return switch (targetType.getSimpleName()) {
                    case "int", "Integer" -> (R) Integer.valueOf(num.intValue());
                    case "long", "Long" -> (R) Long.valueOf(num.longValue());
                    case "double", "Double" -> (R) Double.valueOf(num.doubleValue());
                    case "float", "Float" -> (R) Float.valueOf(num.floatValue());
                    case "short", "Short" -> (R) Short.valueOf(num.shortValue());
                    case "byte", "Byte" -> (R) Byte.valueOf(num.byteValue());
                    default -> (R) num;
                };
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return (R) Boolean.valueOf(val.asBoolean());
            }
            if (targetType.isAssignableFrom(org.graalvm.polyglot.Value.class)) {
                return (R) val;
            }
            return val.as(targetType);
        }

        // звичайний сценарій — тип уже Java
        if (!targetType.isAssignableFrom(type)) {
            throw new ClassCastException(
                    "Cannot cast " + type.getName() + " to " + targetType.getName()
            );
        }

        return (R) value;
    }



    @Override
    public String toString() {
        return "EvalResult[type=%s, value=%s]".formatted(
                type != null ? type.getSimpleName() : "null",
                value
        );
    }

}
