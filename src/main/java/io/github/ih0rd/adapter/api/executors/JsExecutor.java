package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.SupportedLanguage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

/// # JsExecutor
///
/// GraalJS executor implementation for JavaScript code execution.
///
/// ---
/// ## Features
/// - Inline JS code evaluation.
/// - Function-based execution from JS files.
/// - Compatible with Node.js mode (`withNodeSupport()`).
///
/// ---
/// ## Example
/// ```java
/// try (var exec = JsExecutor.createDefault()) {
///     var api = exec.bind(MyJsApi.class);
///     var res = api.add(2, 3);
///     System.out.println(res); // → 5
/// }
/// ```
public final class JsExecutor extends BaseExecutor {

  private static final java.util.Map<Class<?>, Source> JS_SOURCE_CACHE = new ConcurrentHashMap<>();

  public JsExecutor(Context context, Path resourcesPath) {
    super(context, resourcesPath);
  }

  /// ## createDefault
  /// Creates a default JavaScript executor using default GraalJS context.
  public static JsExecutor createDefault() {
    return BaseExecutor.createDefault(SupportedLanguage.JS, JsExecutor::new);
  }

  /// ## create
  /// Creates a JavaScript executor using a custom context builder.
  public static JsExecutor create(PolyglotContextFactory.Builder builder) {
    return BaseExecutor.create(builder, JsExecutor::new);
  }

  /// ## languageId
  /// Returns the GraalVM language identifier (`"js"`).
  @Override
  public String languageId() {
    return SupportedLanguage.JS.id();
  }

  /// ## evaluate(methodName, memberTargetType, args)
  /// Executes a JS function either from global scope or from a loaded JS file.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
    try {
      var source = getFileSource(memberTargetType);
      context.eval(source);
      return callFunction(methodName, args);
    } catch (Exception e) {
      throw new EvaluationException("Error executing JS function: " + methodName, e);
    }
  }

  /// ## evaluate(methodName, memberTargetType)
  /// Executes a JS function without arguments.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
    return evaluate(methodName, memberTargetType, new Object[0]);
  }

  /// ## getFileSource
  /// Loads a JavaScript source file associated with the given interface type.
  private <T> Source getFileSource(Class<T> memberTargetType) {
    return SOURCE_CACHE.computeIfAbsent(
        memberTargetType,
        cls -> {
          return loadScript(SupportedLanguage.JS, cls.getSimpleName());
        });
  }

  /// ## loadSource
  /// Loads and compiles a JavaScript source file associated with the given interface type.
  ///
  /// The method first attempts to locate the script on the application classpath
  /// under `resources/js/`, and if not found, falls back to the resolved
  /// filesystem path provided by the `resourcesPath`.
  ///
  /// The resulting {@link Source} is cached in memory for future executions.
  ///
  /// Example lookup order:
  /// 1. `classpath:/js/MyApi.js`
  /// 2. `${resourcesPath}/MyApi.js`

  private <T> Source loadSource(Class<T> memberTargetType) {
    String jsFileName = memberTargetType.getSimpleName() + ".js";
    String resourcePath = "js/" + jsFileName;
    var cl = Thread.currentThread().getContextClassLoader();

    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is != null) {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          return Source.newBuilder(languageId(), reader, resourcePath).buildLiteral();
        }
      }
    } catch (IOException e) {
      throw new EvaluationException("Failed to read JS script from classpath: " + resourcePath, e);
    }

    Path fsPath = resourcesPath.resolve(jsFileName);
    if (!Files.exists(fsPath)) {
      throw new EvaluationException("Cannot find JS file: " + jsFileName);
    }

    return Source.newBuilder(languageId(), fsPath.toFile()).buildLiteral();
  }

  /// ## evaluate(code)
  /// Evaluates inline JS code and returns unwrapped value.
  @Override
  public <T> Value evaluate(String code) {
    try {
      return context.eval(Source.newBuilder(languageId(), code, "inline.js").buildLiteral());
    } catch (Exception e) {
      throw new EvaluationException("Error during JS inline execution", e);
    }
  }
}
