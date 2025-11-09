package io.github.ih0rd.adapter.api.executors;

import static io.github.ih0rd.adapter.utils.CommonUtils.*;
import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.*;

import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.context.SupportedLanguage;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

/**
 * Optimized Python executor for GraalPy. Extends BaseExecutor to handle Python class bindings and
 * method evaluation.
 */
public final class PyExecutor extends BaseExecutor {

  private static final Map<Class<?>, Object> INSTANCE_CACHE = new ConcurrentHashMap<>();

  public PyExecutor(Context context, Path resourcesPath) {
    super(context, resourcesPath);
  }

  @Override
  public String languageId() {
    return SupportedLanguage.PYTHON.id();
  }

  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
    var instance = mapValue(memberTargetType);
    return invokeMethod(memberTargetType, instance, methodName, args);
  }

  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
    var instance = mapValue(memberTargetType);
    return invokeMethod(memberTargetType, instance, methodName);
  }

  public static PyExecutor createDefault() {
    return createDefault(SupportedLanguage.PYTHON, PyExecutor::new);
  }

  public static PyExecutor create(PolyglotContextFactory.Builder builder) {
    return create(builder, PyExecutor::new);
  }

  private <T> T mapValue(Class<T> memberTargetType) {
    Object cached = INSTANCE_CACHE.get(memberTargetType);
    if (cached != null) {
      return memberTargetType.cast(cached);
    }

    var source = getFileSource(memberTargetType);
    context.eval(source);

    var bindings = context.getPolyglotBindings();
    var pyClass = getFirstElement(bindings.getMemberKeys());
    validate(pyClass, memberTargetType);

    var member = bindings.getMember(pyClass);
    T instance = member.newInstance().as(memberTargetType);

    INSTANCE_CACHE.put(memberTargetType, instance);
    return instance;
  }

  private <T> void validate(String pyClassName, Class<T> memberTargetType) {
    if (pyClassName == null || pyClassName.isEmpty()) {
      throw new EvaluationException("Invalid Python class name: " + pyClassName);
    }
    var interfaceName = memberTargetType.getSimpleName();
    if (!interfaceName.equals(pyClassName)) {
      throw new EvaluationException(
          "Interface name '%s' must equal Python class name '%s'"
              .formatted(interfaceName, pyClassName));
    }
  }

  /// ## getFileSource
  /// Loads a Python source file associated with the given interface type.
  private <T> Source getFileSource(Class<T> memberTargetType) {
    return SOURCE_CACHE.computeIfAbsent(
        memberTargetType,
        cls -> {
          var interfaceName = cls.getSimpleName();
          return loadScript(SupportedLanguage.PYTHON, camelToSnake(interfaceName));
        });
  }
}
