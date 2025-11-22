package io.github.ih0rd.adapter.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.EvaluationException;

/// # BaseExecutor
///
/// Common base class for language-specific executors.
///
/// Responsibilities:
/// - Hold a GraalVM {@link Context} and a filesystem {@link Path} for resources
/// - Provide a generic {@link #bind(Class)} implementation via dynamic proxies
/// - Provide inline evaluation and script loading helpers
public abstract class BaseExecutor implements AutoCloseable {

  /// ### context
  /// Underlying GraalVM {@link Context} instance.
  private final Context context;

  /// ### resourcesPath
  /// Base filesystem path for language-specific scripts (Python/JS files).
  protected final Path resourcesPath;

  /// ### BaseExecutor
  ///
  /// @param context       GraalVM {@link Context} instance
  /// @param resourcesPath base path for guest language resources
  protected BaseExecutor(Context context, Path resourcesPath) {
    this.context = context;
    this.resourcesPath = resourcesPath;
  }

  /// ### languageId
  ///
  /// @return GraalVM language id (e.g. {@code "python"}, {@code "js"})
  protected abstract String languageId();

  /// ### evaluate(methodName, memberTargetType, args)
  ///
  /// Executes a guest-language method by name with arguments.
  ///
  /// @param methodName        guest function/method name
  /// @param memberTargetType  Java interface type being bound
  /// @param args              call arguments
  /// @param <T>               interface type
  /// @return raw {@link Value} result
  protected abstract <T> Value evaluate(
      String methodName, Class<T> memberTargetType, Object... args);

  /// ### evaluate(methodName, memberTargetType)
  ///
  /// Executes a guest-language method by name without arguments.
  ///
  /// @param methodName        guest function/method name
  /// @param memberTargetType  Java interface type being bound
  /// @param <T>               interface type
  /// @return raw {@link Value} result
  protected abstract <T> Value evaluate(String methodName, Class<T> memberTargetType);

  /// ### evaluate(code)
  ///
  /// Evaluates inline guest-language code in this context.
  ///
  /// ```java
  /// Value result = executor.evaluate("1 + 2");
  /// int sum = result.asInt();
  /// ```
  ///
  /// @param code guest language source code
  /// @return evaluation result as {@link Value}
  public Value evaluate(String code) {
    try {
      Source source =
          Source.newBuilder(languageId(), code, "inline." + languageId()).buildLiteral();
      return context.eval(source);
    } catch (Exception e) {
      throw new EvaluationException("Error during " + languageId() + " code execution", e);
    }
  }

  /// ### bind
  ///
  /// Creates a dynamic proxy for the given interface.
  ///
  /// Each interface method is mapped to a guest-language function with the same name.
  ///
  /// @param iface interface to bind
  /// @param <T>   interface type
  /// @return proxy instance backed by guest-language implementation
  @SuppressWarnings("unchecked")
  public <T> T bind(Class<T> iface) {
    return (T)
        Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[] {iface},
            (_, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
              }
              String methodName = method.getName();
              Object[] safeArgs = (args != null ? args : new Object[0]);
              Value result = evaluate(methodName, iface, safeArgs);
              if (result == null || result.isNull()) {
                return null;
              }
              return result.as(method.getReturnType());
            });
  }

  /// ### createDefault
  ///
  /// Creates a default executor instance:
  /// - resolves {@link Path} via {@link ResourcesProvider}
  /// - creates a {@link Context} via {@link PolyglotHelper}
  /// - constructs the executor with {@code (context, resourcesPath)}
  ///
  /// @param language    guest language
  /// @param constructor executor constructor taking {@link Context} and {@link Path}
  /// @param <E>         executor subtype
  /// @return configured executor instance
  protected static <E extends BaseExecutor> E createDefault(
      SupportedLanguage language, BiFunction<Context, Path, E> constructor) {

    Path resourcesPath = ResourcesProvider.get(language);
    Context context = PolyglotHelper.newContext(language);
    return constructor.apply(context, resourcesPath);
  }

  /// ### context
  ///
  /// @return underlying {@link Context}
  protected Context context() {
    return context;
  }

  /// ### callFunction
  ///
  /// Invokes a global function from the language bindings.
  ///
  /// @param methodName guest function name
  /// @param args       call arguments
  /// @return result as {@link Value}
  protected Value callFunction(String methodName, Object... args) {
    try {
      Value bindings = context.getBindings(languageId());
      Value fn = bindings.getMember(methodName);

      if (fn == null || !fn.canExecute()) {
        throw new EvaluationException("Function not found: " + methodName);
      }
      return fn.execute(args);
    } catch (Exception e) {
      throw new EvaluationException("Error executing function: " + methodName, e);
    }
  }

  /// ### loadScript
  ///
  /// Loads a script source for the given language and logical name.
  ///
  /// Resolution order:
  /// 1. classpath resource at: {@code <lang>/<name><ext>}
  /// 2. filesystem resource at: {@code resourcesPath/name.ext}
  ///
  /// @param language guest language
  /// @param name     logical script name (without extension)
  /// @return compiled {@link Source}
  protected Source loadScript(SupportedLanguage language, String name) {
    String fileName = name + language.ext();
    String resourcePath = language.name().toLowerCase() + "/" + fileName;
    ClassLoader cl = Thread.currentThread().getContextClassLoader();

    // classpath lookup
    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is != null) {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          return Source.newBuilder(language.id(), reader, resourcePath).buildLiteral();
        }
      }
    } catch (IOException e) {
      throw new EvaluationException("Failed to read script from classpath: " + resourcePath, e);
    }

    // filesystem fallback
    Path fsPath = resourcesPath.resolve(fileName);
    if (!Files.exists(fsPath)) {
      throw new EvaluationException(
          "Cannot find script: %s (classpath '%s', filesystem '%s')"
              .formatted(fileName, resourcePath, fsPath));
    }

    try {
      return Source.newBuilder(language.id(), fsPath.toFile()).build();
    } catch (IOException e) {
      throw new EvaluationException("Failed to build source from file: " + fsPath, e);
    }
  }

  /// ### close
  ///
  /// Closes the underlying {@link Context}.
  @Override
  public void close() {
    context.close();
  }
}
