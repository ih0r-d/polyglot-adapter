package io.github.ih0rd.adapter.utils;

import org.graalvm.polyglot.Value;

import java.util.*;

/**
 * Utility class for safely converting {@link Value} objects from GraalVM
 * to strongly-typed Java representations.
 */
public final class ValueUnwrapper {
    private ValueUnwrapper() {
    }

    /**
     * Fully generic, strongly typed unwrap method.
     *
     * @param value      the GraalVM {@link Value} to unwrap
     * @param targetType the expected Java type
     * @param <T>        inferred Java type
     * @return converted value of type {@code T}
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Value value, Class<T> targetType) {
        if (value == null || value.isNull()) {
            return null;
        }

        // --- handle primitives ---
        if (targetType == String.class && value.isString()) {
            return (T) value.asString();
        }
        if ((targetType == Boolean.class || targetType == boolean.class) && value.isBoolean()) {
            return (T) Boolean.valueOf(value.asBoolean());
        }
        if ((Number.class.isAssignableFrom(targetType) || targetType.isPrimitive()) && value.isNumber()) {
            Number n = value.as(Number.class);
            return switch (targetType.getSimpleName()) {
                case "int", "Integer" -> (T) Integer.valueOf(n.intValue());
                case "long", "Long" -> (T) Long.valueOf(n.longValue());
                case "double", "Double" -> (T) Double.valueOf(n.doubleValue());
                case "float", "Float" -> (T) Float.valueOf(n.floatValue());
                case "short", "Short" -> (T) Short.valueOf(n.shortValue());
                case "byte", "Byte" -> (T) Byte.valueOf(n.byteValue());
                default -> (T) n;
            };
        }

        // --- arrays ---
        if (value.hasArrayElements() && targetType.isAssignableFrom(List.class)) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(unwrap(value.getArrayElement(i), Object.class));
            }
            return (T) Collections.unmodifiableList(list);
        }

        // --- objects/maps ---
        if (value.hasMembers() && targetType.isAssignableFrom(Map.class)) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, unwrap(value.getMember(key), Object.class));
            }
            return (T) Collections.unmodifiableMap(map);
        }

        // --- callable / host / fallback ---
        if (value.isHostObject()) {
            return (T) value.asHostObject();
        }
        if (targetType.isAssignableFrom(Value.class)) {
            return (T) value;
        }

        // fallback: delegate to Graal built-in conversion
        return value.as(targetType);
    }

    /**
     * Convenience overload — infers {@code <T>} dynamically and returns best-fit Java type.
     * Similar to {@code unwrap(value, Object.class)} but automatically infers type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Value value) {
        if (value == null || value.isNull()) return null;

        if (value.isBoolean()) return (T) Boolean.valueOf(value.asBoolean());
        if (value.isNumber()) {
            Number n = value.as(Number.class);
            Object numValue = (n.doubleValue() == n.intValue())
                    ? Integer.valueOf(n.intValue())
                    : Double.valueOf(n.doubleValue());
            return (T) numValue;
        }
        if (value.isString()) return (T) value.asString();

        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(unwrap(value.getArrayElement(i)));
            }
            return (T) Collections.unmodifiableList(list);
        }

        if (value.hasMembers()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, unwrap(value.getMember(key)));
            }
            return (T) Collections.unmodifiableMap(map);
        }

        if (value.isHostObject()) return (T) value.asHostObject();
        return (T) value;
    }
}
