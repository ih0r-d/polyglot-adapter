package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.api.context.ResourcesProvider;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import io.github.ih0rd.adapter.utils.ValueUnwrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/// # BaseExecutor
///
/// Abstract superclass for all GraalVM language executors (e.g., Python, JS).
///
/// ---
/// ## Responsibilities
/// - Manage shared caches and virtual-thread execution pool.
/// - Provide synchronous and asynchronous evaluation methods.
/// - Expose Java interface binding for polyglot classes (`bind()`).
/// - Handle resource and context lifecycle.
///
/// ---
/// ## Example
/// ```java
/// try (var executor = PyExecutor.createDefault()){
///     MyApi api = executor.bind(MyApi.class);
///     var result = api.ping();
///     System.out.println(result);
/// }
/// ```
public abstract class BaseExecutor implements AutoCloseable {

  /// ### SOURCE_CACHE
  /// Cache of preloaded polyglot script sources.
  protected static final Map<Class<?>, Source> SOURCE_CACHE = new ConcurrentHashMap<>();

  /// ### METHOD_CACHE
  /// Cache of reflected method names for bound interfaces.
  protected static final Map<Method, String> METHOD_CACHE = new ConcurrentHashMap<>();

  /// ### VIRTUAL_EXECUTOR
  /// Global virtual-thread executor shared across all polyglot contexts.
  protected static final ExecutorService VIRTUAL_EXECUTOR =
      Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

  protected final Context context;
  protected final Path resourcesPath;

  protected BaseExecutor(Context context, Path resourcesPath) {
    this.context = context;
    this.resourcesPath = resourcesPath;
  }

  /// ### languageId
  /// Returns the GraalVM language ID (e.g. `"python"` or `"js"`).
  public abstract String languageId();

  /// ### evaluate(methodName, memberTargetType, args)
  /// Invokes a polyglot method by name with provided arguments.
  protected abstract <T> EvalResult<?> evaluate(
      String methodName, Class<T> memberTargetType, Object... args);

  /// ### evaluate(methodName, memberTargetType)
  /// Invokes a no-argument polyglot method.
  protected abstract <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType);

  /// ### evaluate(code)
  /// Executes raw inline code for the current language context.
  ///
  /// Returns an {@link EvalResult} with automatically unwrapped value.
  ///
  /// ```java
  /// var result = executor.evaluate("1 + 2");
  /// System.out.println(result.as(Integer.class)); // → 3
  /// ```
  public <T> EvalResult<?> evaluate(String code) {
    try {
      Value value =
          context.eval(
              Source.newBuilder(languageId(), code, "inline." + languageId()).buildLiteral());
      if (value == null || value.isNull()) {
        return EvalResult.of(null);
      }
      T unwrapped = ValueUnwrapper.unwrap(value);
      return EvalResult.of(unwrapped);
    } catch (Exception e) {
      throw new EvaluationException("Error during " + languageId() + " code execution", e);
    }
  }

  /// ### async(action)
  /// Submits the given task to the shared virtual executor.
  ///
  /// Returns a {@link CompletableFuture} of the task result.
  ///
  /// ```java
  /// CompletableFuture<Integer> asyncResult = executor.async(() -> 42);
  /// ```
  public <R> CompletableFuture<R> async(Supplier<R> action) {
    return CompletableFuture.supplyAsync(action, VIRTUAL_EXECUTOR);
  }

  /// ### bind(iface)
  /// Dynamically binds a Java interface to a polyglot object implementation.
  ///
  /// Each method call on the interface maps to a polyglot method invocation.
  ///
  /// ```java
  /// MyApi api = executor.bind(MyApi.class);
  /// var response = api.add(1, 2);
  /// ```
  @SuppressWarnings("unchecked")
  public <T> T bind(Class<T> iface) {
    return (T)
        Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class[] {iface},
            (_, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
              }
              String methodName = METHOD_CACHE.computeIfAbsent(method, Method::getName);
              var result = evaluate(methodName, iface, args);
              return result != null ? result.value() : null;
            });
  }

  /// ### createDefault
  /// Builds a default executor for the specified language.
  ///
  /// ```java
  /// var pyExec = BaseExecutor.createDefault(Language.PYTHON, PyExecutor::new);
  /// ```
  protected static <E extends BaseExecutor> E createDefault(
      Language lang, BiFunction<Context, Path, E> constructor) {
    var builder = new PolyglotContextFactory.Builder(lang);
    return constructor.apply(builder.build(), builder.getResourcesPath());
  }

  /// ### create
  /// Builds an executor from a custom {@link PolyglotContextFactory.Builder}.
  protected static <E extends BaseExecutor> E create(
      Language lang,
      PolyglotContextFactory.Builder builder,
      BiFunction<Context, Path, E> constructor) {
    return constructor.apply(builder.build(), builder.getResourcesPath());
  }

  /// ### context
  /// Returns the underlying GraalVM {@link Context}.
  protected Context context() {
    return context;
  }

  /// ## callFunction
  /// Generic function invoker for polyglot values. Used by all executors that expose callable
  // members (e.g., JS,
  /// Python).
  protected <T> EvalResult<?> callFunction(String methodName, Object... args) {
    try {
      Value bindings = context.getBindings(languageId());
      Value fn = bindings.getMember(methodName);

      if (fn == null || !fn.canExecute()) {
        throw new EvaluationException("Function not found: " + methodName);
      }

      Value result = fn.execute(args);
      if (result == null || result.isNull()) {
        return EvalResult.of(null);
      }

      T unwrapped = io.github.ih0rd.adapter.utils.ValueUnwrapper.unwrap(result);
      return EvalResult.of(unwrapped);
    } catch (Exception e) {
      throw new EvaluationException("Error executing function: " + methodName, e);
    }
  }

  /// ## loadScript
  /// Loads and compiles a polyglot source file for the specified language.
  ///
  /// ---
  /// ### Description
  /// Resolves the base path via {@link ResourcesProvider}, then attempts:
  /// 1. Classpath resource lookup (for packaged JAR resources)
  /// 2. Filesystem lookup (for dev / runtime overrides)
  ///
  /// Supports overrides via:
  /// `-Dpy.polyglot-resources.path` or `-Djs.polyglot-resources.path`.
  ///
  /// ---
  /// ### Example
  /// ```java
  /// var source = loadScript(Language.PYTHON, "my_api");
  /// context.eval(source);
  /// ```
  protected Source loadScript(Language lang, String name) {
    String fileName = name + lang.ext();
    Path basePath = ResourcesProvider.get(lang);
    String resourcePath = lang.name().toLowerCase() + "/" + fileName;
    var cl = Thread.currentThread().getContextClassLoader();

    try (InputStream is = cl.getResourceAsStream(resourcePath)) {
      if (is != null) {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          return Source.newBuilder(lang.id(), reader, resourcePath).buildLiteral();
        }
      }
    } catch (IOException e) {
      throw new EvaluationException("Failed to read script from classpath: " + resourcePath, e);
    }

    Path fsPath = basePath.resolve(fileName);
    if (!Files.exists(fsPath)) {
      throw new EvaluationException(
          "Cannot find script: %s (classpath '%s', filesystem '%s')"
              .formatted(fileName, resourcePath, fsPath));
    }

    return Source.newBuilder(lang.id(), fsPath.toFile()).buildLiteral();
  }

  /// ### close
  /// Closes the underlying polyglot {@link Context} and releases resources.
  @Override
  public void close() {
    context.close();
  }
}
