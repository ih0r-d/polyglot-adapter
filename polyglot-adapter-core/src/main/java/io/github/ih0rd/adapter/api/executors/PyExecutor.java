package io.github.ih0rd.adapter.api.executors;

import static io.github.ih0rd.adapter.utils.CommonUtils.*;
import static io.github.ih0rd.adapter.utils.Constants.PYTHON;
import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * Executor for Python classes via GraalPy. Loads sources from either classpath (resources/python)
 * or a filesystem path resolved via {@link PolyglotContextFactory.Builder#getResourcesPath()}.
 */
public record PyExecutor(Context context, Path resourcesPath) implements BaseExecutor {

  /** Create executor with default context configuration. */
  public static PyExecutor createDefault() {
    var builder = new PolyglotContextFactory.Builder(Language.PYTHON);
    return new PyExecutor(builder.build(), builder.getResourcesPath());
  }

  /** Create executor with custom context configuration. */
  public static PyExecutor create(PolyglotContextFactory.Builder builder) {
    return new PyExecutor(builder.build(), builder.getResourcesPath());
  }

  @Override
  public <T> EvalResult<?> evaluate(
      String methodName, Class<T> memberTargetType, Object... args) {
    var type = mapValue(memberTargetType, methodName);
    return invokeMethod(memberTargetType, type, methodName, args);
  }

  @Override
  public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType) {
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

  /** Resolve Python source file: check classpath first, then fallback to filesystem path. */
  private <T> Source getFileSource(Class<T> memberTargetType) {
    var interfaceName = memberTargetType.getSimpleName();
    var pyFileName = camelToSnake(interfaceName);
    var resourcePath = "python/" + pyFileName + ".py";
    var cl = Thread.currentThread().getContextClassLoader();

    // 1) Classpath
    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is != null) {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          return Source.newBuilder(PYTHON, reader, resourcePath).build();
        }
      }
    } catch (Exception e) {
      throw new EvaluationException(
          "Failed to read Python script from classpath: " + resourcePath, e);
    }

    // 2) Filesystem fallback
    var fsPath = resourcesPath.resolve(pyFileName + ".py");
    if (!Files.exists(fsPath)) {
      throw new EvaluationException(
          "Cannot find Python file: "
              + pyFileName
              + " (classpath '"
              + resourcePath
              + "', filesystem '"
              + fsPath
              + "')");
    }

    try {
      return Source.newBuilder(PYTHON, fsPath.toFile()).build();
    } catch (Exception e) {
      throw new EvaluationException("Could not load Python file: " + pyFileName, e);
    }
  }

  @Override
  public void close() {
    context.close();
  }
}
