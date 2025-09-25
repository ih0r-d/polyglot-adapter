package io.github.ih0r.adapter.api.executors;

import static io.github.ih0r.adapter.utils.CommonUtils.*;
import static io.github.ih0r.adapter.utils.Constants.*;

import io.github.ih0r.adapter.api.context.Language;
import io.github.ih0r.adapter.api.context.PolyglotContextFactory;
import io.github.ih0r.adapter.exceptions.EvaluationException;
import io.github.ih0r.adapter.utils.StringCaseConverter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

/** Internal executor for Python classes via GraalPy. Implements BaseExecutor contract. */
public record PyExecutor(Context context) implements BaseExecutor {

  /** Create executor with default context configuration. */
  public static PyExecutor createDefault() {
    return new PyExecutor(PolyglotContextFactory.createDefault(Language.PYTHON));
  }

  /**
   * Create executor with custom context configuration.
   *
   * @param builder PolyglotContextFactory.Builder for fine-tuned setup
   */
  public static PyExecutor create(PolyglotContextFactory.Builder builder) {
    return new PyExecutor(builder.build());
  }

  @Override
  public <T> Map<String, Object> evaluate(
      String methodName, Class<T> memberTargetType, Object... args) {
    var type = mapValue(memberTargetType, methodName);
    return invokeMethod(memberTargetType, type, methodName, args);
  }

  @Override
  public <T> Map<String, Object> evaluate(String methodName, Class<T> memberTargetType) {
    var type = mapValue(memberTargetType, methodName);
    return invokeMethod(memberTargetType, type, methodName);
  }

  private <T> T mapValue(Class<T> memberTargetType, String methodName) {
    var source = getFileSource(memberTargetType);
    context.eval(source);

    var polyglotBindings = context.getPolyglotBindings();
    var pyClass = getFirstElement(polyglotBindings.getMemberKeys());
    validate(pyClass, memberTargetType);

    var member = polyglotBindings.getMember(pyClass);
    if (!checkIfMethodExists(memberTargetType, methodName)
        || member.getMember(methodName) == null) {
      throw new EvaluationException(
          "Method " + methodName + " is not supported for " + memberTargetType);
    }
    return member.newInstance().as(memberTargetType);
  }

  private <T> void validate(String pyClassName, Class<T> memberTargetType) {
    if (pyClassName == null || pyClassName.isEmpty()) {
      throw new EvaluationException("Invalid Python class name: " + pyClassName);
    }
    var interfaceName = memberTargetType.getSimpleName();
    if (!interfaceName.equals(pyClassName)) {
      throw new EvaluationException(
          "Interface name '"
              + interfaceName
              + "' must equal Python class name '"
              + pyClassName
              + "'");
    }
  }

  private <T> Source getFileSource(Class<T> memberTargetType) {
    var interfaceName = memberTargetType.getSimpleName();
    var pyFileName = StringCaseConverter.camelToSnake(interfaceName);
    var resourcePath = "python/" + pyFileName + ".py";
    var cl = Thread.currentThread().getContextClassLoader();

    // 1) Load from JAR resources
    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is != null) {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          return Source.newBuilder(PYTHON, reader, resourcePath).build();
        }
      }
    } catch (Exception e) {
      throw new EvaluationException("Failed to read Python script: " + resourcePath, e);
    }

    // 2) Fallback: load from local filesystem
    var optionalPath = checkFileExists(pyFileName);
    if (optionalPath.isEmpty()) {
      throw new EvaluationException(
          "Cannot find Python file: "
              + pyFileName
              + " (classpath '"
              + resourcePath
              + "' or local '"
              + PROJ_PY_RESOURCES_PATH
              + "')");
    }

    try {
      return Source.newBuilder(PYTHON, optionalPath.get().toFile()).build();
    } catch (Exception e) {
      throw new EvaluationException("Could not load Python file: " + pyFileName, e);
    }
  }

  @Override
  public void close() {
    context.close();
  }
}
