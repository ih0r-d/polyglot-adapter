package io.github.ih0rd.adapter.context;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import io.github.ih0rd.adapter.exceptions.InvocationException;
import io.github.ih0rd.adapter.exceptions.ScriptNotFoundException;
import io.github.ih0rd.adapter.script.ScriptSource;

/// # AbstractPolyglotExecutor
///
/// Common base class for language-specific polyglot executors.
///
/// Responsibilities:
/// - Hold a GraalVM {@link Context} instance
/// - Delegate script resolution and loading to {@link ScriptSource}
/// - Provide dynamic interface binding via Java proxies
/// - Encapsulate common execution and invocation behavior
///
/// Design notes:
/// - This class does not perform any filesystem or classpath access
/// - All script I/O is delegated to the {@link ScriptSource} SPI
/// - Executors are responsible only for execution, not resource resolution
///
public abstract class AbstractPolyglotExecutor implements AutoCloseable {

  /// ### context
  /// Underlying GraalVM {@link Context} instance.
  ///
  /// Lifecycle:
  /// - If created internally, the executor owns this context and will close it
  /// - If provided externally, the caller is responsible for lifecycle management
  protected final Context context;

  /// ### scriptSource
  /// Abstraction responsible for resolving and opening script sources.
  protected final ScriptSource scriptSource;

  /// ### sourceCache
  /// Per-executor cache of compiled {@link Source} instances.
  protected final Map<Class<?>, Source> sourceCache = new ConcurrentHashMap<>();

  /// ### AbstractPolyglotExecutor
  ///
  /// @param context       GraalVM {@link Context} instance (must not be null)
  /// @param scriptSource script source abstraction (must not be null)
  protected AbstractPolyglotExecutor(Context context, ScriptSource scriptSource) {
    if (context == null) {
      throw new IllegalArgumentException("Context must not be null");
    }
    if (scriptSource == null) {
      throw new IllegalArgumentException("ScriptSource must not be null");
    }
    this.context = context;
    this.scriptSource = scriptSource;
  }

  /// ### languageId
  ///
  /// @return GraalVM language id (e.g. {@code "python"}, {@code "js"})
  protected abstract String languageId();

  /// ### evaluate(methodName, memberTargetType, args)
  ///
  /// Executes a guest-language function by name with arguments.
  ///
  /// @param methodName        guest function name
  /// @param memberTargetType  Java interface type being bound
  /// @param args              call arguments
  /// @param <T>               interface type
  /// @return raw {@link Value} result
  protected abstract <T> Value evaluate(
      String methodName, Class<T> memberTargetType, Object... args);

  /// ### evaluate(methodName, memberTargetType)
  ///
  /// Executes a guest-language function by name without arguments.
  ///
  /// @param methodName       guest function name
  /// @param memberTargetType  Java interface type being bound
  /// @param <T>               interface type
  /// @return raw {@link Value} result
  protected abstract <T> Value evaluate(String methodName, Class<T> memberTargetType);

  /// ### evaluate(code)
  ///
  /// Evaluates inline guest-language code in this context.
  ///
  /// NOTE:
  /// This method is retained for backward compatibility and will be
  /// restricted or removed in a future release.
  ///
  /// @param code guest language source code
  /// @return evaluation result as {@link Value}
  public Value evaluate(String code) {
    try {
      Source source =
          Source.newBuilder(languageId(), code, "inline." + languageId()).buildLiteral();
      return context.eval(source);
    } catch (Exception e) {
      throw new InvocationException("Error during " + languageId() + " inline code execution", e);
    }
  }

  /// ### bind
  ///
  /// Creates a dynamic proxy for the given Java interface.
  ///
  /// Each interface method is mapped to a guest-language function
  /// with the same name.
  ///
  /// @param iface interface to bind
  /// @param <T>   interface type
  /// @return proxy instance backed by guest-language implementation
  @SuppressWarnings("unchecked")
  public <T> T bind(Class<T> iface) {
    if (iface == null) {
      throw new IllegalArgumentException("Interface type must not be null");
    }

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
  /// Language-specific executors may override this method to perform
  /// actual validation during initialization.
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
  /// Loads and compiles a script using the configured {@link ScriptSource}.
  ///
  /// @param language   guest language
  /// @param scriptName logical script name
  /// @return compiled {@link Source}
  protected Source loadScript(SupportedLanguage language, String scriptName) {
    if (!scriptSource.exists(language, scriptName)) {
      throw new ScriptNotFoundException(
          "Script not found: " + scriptName + " for language " + language);
    }

    try (Reader reader = scriptSource.open(language, scriptName)) {
      return Source.newBuilder(language.id(), reader, scriptName).buildLiteral();
    } catch (IOException e) {
      throw new EvaluationException(
          "Failed to load script: " + scriptName + " for language " + language, e);
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

  /// ### metadata
  ///
  /// Returns a metadata snapshot of this executor instance.
  ///
  /// Intended for logging, debugging and health checks.
  ///
  /// @return mutable {@link Map} with metadata key/value pairs
  public Map<String, Object> metadata() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("executorType", getClass().getName());
    info.put("languageId", languageId());
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
