package io.github.ih0rd.adapter.utils;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility methods for reflection and polyglot adapter support.
 *
 * <p>This class provides helper methods to:
 *
 * <ul>
 *   <li>Reflectively invoke methods on Java interfaces backed by GraalVM polyglot objects
 *   <li>Resolve parameter types for methods by name
 *   <li>Check method existence on interfaces
 *   <li>Safely extract the first element from a {@link Set}
 * </ul>
 *
 * <p>All checked/unchecked reflection errors are wrapped in {@link
 * io.github.ih0rd.adapter.exceptions.EvaluationException}.
 */
public final class CommonUtils {

  private CommonUtils() {
    // utility class, do not instantiate
  }

  /**
   * Reflectively invokes a method on the given target instance.
   *
   * <p>Arguments are automatically coerced to match primitive parameter types (e.g., {@link
   * Integer} → {@code int}, {@link Double} → {@code double}).
   *
   * @param targetType the Java interface type associated with the Python class
   * @param targetInstance the polyglot-mapped Java instance
   * @param methodName the method name to call
   * @param args arguments to pass into the method
   * @param <T> the generic Java type
   * @return immutable map with keys:
   *     <ul>
   *       <li>{@code returnType} — the canonical name of the method's return type
   *       <li>{@code result} — the method's return value, or {@link Optional#empty()} if void
   *     </ul>
   *
   * @throws EvaluationException if reflection fails or the method cannot be invoked
   */
  public static <T> EvalResult<?> invokeMethod(
          Class<T> targetType, T targetInstance, String methodName, Object... args) {
      try {
          Method method = getMethodByName(targetType, methodName);
          Object result = method.invoke(targetInstance, coerceArguments(method.getParameterTypes(), args));
          return EvalResult.of(result);
      } catch (Exception e) {
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

  /**
   * Adjusts arguments for reflection by coercing wrapper types to primitives.
   *
   * @param paramTypes expected parameter types of the target method
   * @param args actual arguments provided
   * @return array of coerced arguments
   */
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

  /**
   * Coerces a single argument to match a primitive parameter type.
   *
   * @param primitiveType the expected primitive type
   * @param arg the provided argument
   * @return coerced value
   * @throws IllegalArgumentException if {@code null} is passed for a primitive
   */
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

  public static boolean checkIfMethodExists(Class<?> interfaceClass, String methodName) {
    if (!interfaceClass.isInterface()) {
      throw new EvaluationException(
          "Provided class '" + interfaceClass.getName() + "' must be an interface");
    }

    return Arrays.stream(interfaceClass.getDeclaredMethods())
        .map(Method::getName)
        .anyMatch(name -> name.equals(methodName));
  }

  public static <T> T getFirstElement(Set<T> memberKeys) {
    if (memberKeys == null || memberKeys.isEmpty()) {
      return null;
    }
    return memberKeys.iterator().next();
  }
}
