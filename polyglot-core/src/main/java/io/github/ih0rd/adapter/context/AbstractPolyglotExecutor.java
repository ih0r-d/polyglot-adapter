package io.github.ih0rd.adapter.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import io.github.ih0rd.adapter.exceptions.InvocationException;
import io.github.ih0rd.adapter.exceptions.ScriptNotFoundException;

/// # AbstractPolyglotExecutor
///
/// Common base class for language-specific executors.
///
/// Responsibilities:
/// - hold a GraalVM {@link Context} and a filesystem {@link Path} for resources
/// - provide a generic {@link #bind(Class)} implementation via dynamic proxies
/// - provide inline evaluation, script loading, caching and basic runtime info
public abstract class AbstractPolyglotExecutor implements AutoCloseable {

  /// ### context
  /// Underlying GraalVM {@link Context} instance.
  ///
  /// Lifecycle:
  /// - when created via {@link #createDefault}, the executor owns this context
  ///   and {@link #close()} will close it;
  /// - when created with an external context (e.g. PyExecutor.createWithContext),
  ///   the caller is responsible for context lifecycle.
  protected final Context context;

  /// ### resourcesPath
  /// Base filesystem path for language-specific scripts (Python/JS files).
  protected final Path resourcesPath;

  /// ### sourceCache
  /// Per-executor cache of compiled {@link Source} per interface type.
  protected final Map<Class<?>, Source> sourceCache = new ConcurrentHashMap<>();

  /// ### AbstractPolyglotExecutor
  ///
  /// @param context       GraalVM {@link Context} instance (must not be null)
  /// @param resourcesPath base path for guest language resources (may be null)
  protected AbstractPolyglotExecutor(Context context, Path resourcesPath) {
    if (context == null) {
      throw new IllegalArgumentException("Context must not be null");
    }
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
      throw new InvocationException("Error during " + languageId() + " code execution", e);
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

  /// ### validateBinding
  ///
  /// Validates that the given Java interface can be bound to a guest implementation.
  ///
  /// Default implementation only checks for null and then throws
  /// {@link UnsupportedOperationException}. Language-specific executors
  /// (e.g. Python/JS) should override this to perform real validation.
  ///
  /// @param iface interface to validate
  /// @param <T>   interface type
  public <T> void validateBinding(Class<T> iface) {
    if (iface == null) {
      throw new IllegalArgumentException("Interface type must not be null");
    }
    throw new UnsupportedOperationException(
        "Binding validation is not implemented for executor: " + getClass().getSimpleName());
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
  @SuppressWarnings("resource") // context is closed by executor.close()
  protected static <E extends AbstractPolyglotExecutor> E createDefault(
      SupportedLanguage language, BiFunction<Context, Path, E> constructor) {

    Path resourcesPath = ResourcesProvider.get(language);
    Context context = PolyglotHelper.newContext(language);
    return constructor.apply(context, resourcesPath);
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
        throw new BindingException("Function not found: " + methodName);
      }
      return fn.execute(args);
    } catch (BindingException e) {
      throw e;
    } catch (Exception e) {
      throw new InvocationException("Error executing function: " + methodName, e);
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
      throw new ScriptNotFoundException(
          "Cannot find script: %s (classpath '%s', filesystem '%s')"
              .formatted(fileName, resourcePath, fsPath));
    }

    try {
      return Source.newBuilder(language.id(), fsPath.toFile()).build();
    } catch (IOException e) {
      throw new EvaluationException("Failed to build source from file: " + fsPath, e);
    }
  }

  /// ### clearSourceCache
  ///
  /// Clears the cached sources.
  public void clearSourceCache() {
    sourceCache.clear();
  }

  /// ### clearAllCaches
  ///
  /// Clears all caches maintained by this executor.
  /// Subclasses may override to clear additional caches.
  public void clearAllCaches() {
    clearSourceCache();
  }

  /// ### runtimeInfo
  ///
  /// Returns a metadata snapshot of this executor instance.
  ///
  /// Intended for logging, debugging and health checks.
  ///
  /// Implementations may extend the returned map with
  /// language-specific information (cache sizes, bound interfaces, etc.).
  ///
  /// @return mutable {@link Map} with metadata key/value pairs
  public Map<String, Object> metadata() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("executorType", getClass().getName());
    info.put("languageId", languageId());
    info.put("resourcesPath", resourcesPath != null ? resourcesPath.toString() : null);
    info.put("sourceCacheSize", sourceCache.size());
    return info;
  }

  /// ### close
  ///
  /// Closes the underlying {@link Context}.
  @Override
  public void close() {
    context.close();
  }
}
