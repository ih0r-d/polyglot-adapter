package io.github.ih0rd.adapter.utils;

import java.util.*;

import org.graalvm.polyglot.Value;

/// # ValueUnwrapper
/// Utility class for safely converting {@link org.graalvm.polyglot.Value}
/// objects from GraalVM into strongly-typed Java representations.
///
/// ---
/// ### Responsibilities
/// - Converts polyglot `Value` objects into standard Java types.
/// - Supports primitives, strings, arrays, and maps.
/// - Provides both typed and generic unwrapping methods.
/// - Handles nested polyglot structures recursively.
///
/// ---
/// ### Example
/// ```java
/// Value val = context.eval("python", "[1, 2, 3]");
/// List<Integer> list = ValueUnwrapper.unwrap(val, List.class);
/// ```
///
/// ```java
/// Value val = context.eval("js", "({name: 'Alice', age: 30})");
/// Map<String, Object> user = ValueUnwrapper.unwrap(val);
/// System.out.println(user.get("name")); // "Alice"
/// ```
public final class ValueUnwrapper {

  private ValueUnwrapper() {
    // utility class, do not instantiate
  }

  /// ### unwrap(value, targetType)
  /// Strongly-typed unwrapping method that converts a GraalVM {@link Value}
  /// into the specified Java type.
  ///
  /// ---
  /// #### Parameters
  /// - `value` — GraalVM {@link Value} instance.
  /// - `targetType` — expected Java target type.
  ///
  /// ---
  /// #### Returns
  /// The converted Java object of type `<T>`.
  ///
  /// ---
  /// #### Behavior
  /// - Handles primitives (`int`, `double`, `boolean`, etc.).
  /// - Converts arrays into immutable {@link java.util.List}.
  /// - Converts objects into immutable {@link java.util.Map}.
  /// - Delegates to Graal’s built-in `Value.as()` for unknown types.
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
    if ((Number.class.isAssignableFrom(targetType) || targetType.isPrimitive())
        && value.isNumber()) {
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

    return value.as(targetType);
  }

  /// ### unwrap(value)
  /// Convenience overload that dynamically infers the resulting type.
  ///
  /// ---
  /// #### Behavior
  /// - Automatically detects the most appropriate Java type.
  /// - Handles lists, maps, primitives, and nested objects.
  ///
  /// ---
  /// #### Example
  /// ```java
  /// Value jsValue = context.eval("js", "[10, 20, 30]");
  /// List<?> numbers = ValueUnwrapper.unwrap(jsValue);
  /// ```
  @SuppressWarnings("unchecked")
  public static <T> T unwrap(Value value) {
    if (value == null || value.isNull()) return null;

    if (value.isBoolean()) return (T) Boolean.valueOf(value.asBoolean());
    if (value.isNumber()) {
      Number n = value.as(Number.class);
      Object numValue =
          (n.doubleValue() == n.intValue())
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
