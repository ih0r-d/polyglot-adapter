package io.github.ih0rd.adapter.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.github.ih0rd.adapter.exceptions.EvaluationException;
import org.graalvm.polyglot.Value;

/// # CommonUtils
/// Utility class providing reflection and polyglot adapter helpers.
///
/// ---
/// ### Responsibilities
/// - Fast method invocation using {@link java.lang.invoke.MethodHandle}.
/// - Primitive and wrapper argument coercion for GraalVM calls.
/// - Reflection helpers for method discovery and validation.
/// - Lightweight polyglot utilities (e.g., `getFirstElement()`).
///
/// ---
/// ### Notes
/// All reflection or invocation errors are wrapped in {@link
// io.github.ih0rd.adapter.exceptions.EvaluationException}.
///
/// ---
/// ### Example
/// ```java
/// var result = CommonUtils.invokeMethod(MyApi.class, instance, "ping");
/// System.out.println(result.value());
/// ```
public final class CommonUtils {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
  private static final Map<Method, MethodHandle> HANDLE_CACHE = new ConcurrentHashMap<>();

  private CommonUtils() {
    // utility class, do not instantiate
  }

  /// ### invokeMethod
  /// Reflectively invokes a method on the given target instance using {@link MethodHandle}.
  ///
  /// ---
  /// #### Parameters
  /// - `targetType` — the Java interface type bound to the polyglot class.
  /// - `targetInstance` — the polyglot-mapped Java instance.
  /// - `methodName` — the method name to call.
  /// - `args` — optional arguments to pass.
  ///
  /// ---
  /// #### Returns
  /// {@link Value} containing the unwrapped return value.
  ///
  /// ---
  /// #### Throws
  /// {@link EvaluationException} if reflection fails or the method cannot be invoked.
  public static <T> Value invokeMethod(
          Class<T> targetType, T targetInstance, String methodName, Object... args) {

      try {
          Method method = getMethodByName(targetType, methodName);
          MethodHandle handle =
                  HANDLE_CACHE.computeIfAbsent(
                          method,
                          m -> {
                              try {
                                  return LOOKUP.unreflect(m);
                              } catch (IllegalAccessException e) {
                                  throw new EvaluationException("Cannot unreflect method: " + m, e);
                              }
                          });

          Object result;
          if (args != null && args.length > 0) {
              Object[] coercedArgs = coerceArguments(method.getParameterTypes(), args);
              result = handle.bindTo(targetInstance).invokeWithArguments(coercedArgs);
          } else {
              result = handle.bindTo(targetInstance).invoke();
          }

          // Wrap Java object result into Graal Value for uniformity
          return Value.asValue(result);

      } catch (Throwable e) {
          throw new EvaluationException("Could not invoke method '%s'".formatted(methodName), e);
      }
  }


    private static <T> Method getMethodByName(Class<T> targetType, String methodName)
      throws NoSuchMethodException {
    Class<?>[] parameterTypes = getParameterTypesByMethodName(targetType, methodName);
    return targetType.getMethod(methodName, parameterTypes);
  }

  private static <T> Class<?>[] getParameterTypesByMethodName(
      Class<T> targetType, String methodName) {
    return Arrays.stream(targetType.getDeclaredMethods())
        .filter(method -> method.getName().equals(methodName))
        .findFirst()
        .map(Method::getParameterTypes)
        .orElseThrow(() -> new EvaluationException("Method '" + methodName + "' not found"));
  }

  /// ### coerceArguments
  /// Coerces wrapper arguments to match primitive parameter types before reflective invocation.
  private static Object[] coerceArguments(Class<?>[] paramTypes, Object[] args) {
    Object[] coerced = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      Object arg = args[i];
      Class<?> target = paramTypes[i];

      if (target.isPrimitive()) {
        coerced[i] = coercePrimitive(target, arg);
      } else {
        coerced[i] = arg;
      }
    }
    return coerced;
  }

  /// ### coercePrimitive
  /// Coerces a single argument to a compatible primitive type.
  ///
  /// ---
  /// #### Throws
  /// {@link IllegalArgumentException} if `null` is passed for a primitive parameter.
  private static Object coercePrimitive(Class<?> primitiveType, Object arg) {
    if (arg == null) {
      throw new IllegalArgumentException("Null passed for primitive parameter: " + primitiveType);
    }
    if (!(arg instanceof Number num)) {
      return arg; // let reflection throw if it's not assignable
    }
    return switch (primitiveType.getName()) {
      case "int" -> num.intValue();
      case "long" -> num.longValue();
      case "double" -> num.doubleValue();
      case "float" -> num.floatValue();
      case "short" -> num.shortValue();
      case "byte" -> num.byteValue();
      case "char" -> (char) num.intValue();
      case "boolean" -> (num.intValue() != 0);
      default -> arg;
    };
  }

  /// ### checkIfMethodExists
  /// Checks whether a method exists on a given interface.
  ///
  /// ---
  /// #### Throws
  /// {@link EvaluationException} if the provided class is not an interface.
  public static boolean checkIfMethodExists(Class<?> interfaceClass, String methodName) {
    if (!interfaceClass.isInterface()) {
      throw new EvaluationException(
          "Provided class '" + interfaceClass.getName() + "' must be an interface");
    }

    return Arrays.stream(interfaceClass.getDeclaredMethods())
        .map(Method::getName)
        .anyMatch(name -> name.equals(methodName));
  }

  /// ### getFirstElement
  /// Returns the first element from a given {@link Set}, or `null` if empty.
  public static <T> T getFirstElement(Set<T> memberKeys) {
    if (memberKeys == null || memberKeys.isEmpty()) {
      return null;
    }
    return memberKeys.iterator().next();
  }
}
