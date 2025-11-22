package io.github.ih0rd.adapter.context;

import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.EvaluationException;

/// # PyExecutor
///
/// Python executor for GraalPy (GraalVM 25).
///
/// ---
/// ## Convention
/// For a given Java interface:
/// - interface: `MyService`
/// - Python file: `my_service.py`
/// - Python class: `class MyService: ...`
///
/// The executor:
/// - loads the matching Python module from classpath or filesystem
/// - locates the Python class via polyglot or language bindings
/// - instantiates the class and caches the instance per interface type
public final class PyExecutor extends BaseExecutor {

  /// ### instanceCache
  ///
  /// Per-executor cache of Python instances keyed by Java interface type.
  ///
  /// Uses {@link WeakReference} to avoid keeping guest objects alive
  /// longer than the underlying context.
  private final Map<Class<?>, WeakReference<Value>> instanceCache = new ConcurrentHashMap<>();

  /// ### sourceCache
  ///
  /// Per-executor cache of compiled {@link Source} per interface type.
  private final Map<Class<?>, Source> sourceCache = new ConcurrentHashMap<>();

  /// ### PyExecutor
  ///
  /// Low-level constructor. Prefer using factory methods:
  /// - {@link #createDefault()}
  /// - {@link #create(Consumer)}
  /// - {@link #createWithContext(Context)}
  ///
  /// @param context       GraalPy {@link Context} instance
  /// @param resourcesPath base path for Python scripts
  public PyExecutor(Context context, Path resourcesPath) {
    super(context, resourcesPath);
  }

  /// ### languageId
  ///
  /// @return GraalVM language id for Python (`"python"`).
  @Override
  public String languageId() {
    return SupportedLanguage.PYTHON.id();
  }

  /// ### evaluate(methodName, memberTargetType, args)
  ///
  /// Invokes a Python instance method with arguments.
  ///
  /// Resolution:
  /// 1. Resolve (or create) Python instance for the given interface.
  /// 2. Invoke method via {@link #invokeMember(Value, String, Object...)}.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
    Value instance = resolveInstance(memberTargetType);
    return invokeMember(instance, methodName, args);
  }

  /// ### evaluate(methodName, memberTargetType)
  ///
  /// Invokes a Python instance method without arguments.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
    Value instance = resolveInstance(memberTargetType);
    return invokeMember(instance, methodName);
  }

  // ======================================================================
  // Factory methods
  // ======================================================================

  /// ### createDefault
  ///
  /// Creates a default Python executor:
  /// - context is created via {@link PolyglotHelper#newContext(SupportedLanguage)}
  /// - resources path is resolved via {@link ResourcesProvider}.
  ///
  /// ```java
  /// try (PyExecutor exec = PyExecutor.createDefault()) {
  ///   MyApi api = exec.bind(MyApi.class);
  /// }
  /// ```
  ///
  /// @return configured {@link PyExecutor} instance
  public static PyExecutor createDefault() {
    return createDefault(SupportedLanguage.PYTHON, PyExecutor::new);
  }

  /// ### create
  ///
  /// Creates a Python executor with a customized {@link Context.Builder}.
  ///
  /// ```java
  /// PyExecutor exec =
  ///     PyExecutor.create(b -> b
  ///         .option("engine.WarnInterpreterOnly", "false")
  ///         .option("python.WarnExperimentalFeatures", "false"));
  /// ```
  ///
  /// @param customizer optional builder customizer (may be {@code null})
  /// @return configured {@link PyExecutor} instance
  public static PyExecutor create(Consumer<Context.Builder> customizer) {
    Context context = PolyglotHelper.newContext(SupportedLanguage.PYTHON, customizer);
    return createWithContext(context);
  }

  /// ### createWithContext
  ///
  /// Creates a {@link PyExecutor} using a pre-configured {@link Context}.
  ///
  /// `resourcesPath` is resolved automatically via {@link ResourcesProvider}
  /// for {@link SupportedLanguage#PYTHON}.
  ///
  /// ```java
  /// Context ctx = PolyglotHelper.newContext(
  ///     SupportedLanguage.PYTHON,
  ///     b -> b.option("python.WarnExperimentalFeatures", "false")
  /// );
  ///
  /// try (PyExecutor exec = PyExecutor.createWithContext(ctx)) {
  ///   MyApi api = exec.bind(MyApi.class);
  /// }
  /// ```
  ///
  /// @param context existing GraalPy {@link Context}
  /// @return configured {@link PyExecutor} instance
  public static PyExecutor createWithContext(Context context) {
    Path resourcesPath = ResourcesProvider.get(SupportedLanguage.PYTHON);
    return new PyExecutor(context, resourcesPath);
  }

  // ======================================================================
  // Core resolution logic
  // ======================================================================

  /// ### resolveInstance
  ///
  /// Resolves or creates a Python instance for a given Java interface type.
  ///
  /// Steps:
  /// 1. Look up cached instance in {@link #instanceCache}.
  /// 2. If missing or collected:
  ///    - load and `eval` the Python module for the interface (e.g. `my_service.py`)
  ///    - locate the Python class via {@link #resolveClass(Class)}
  ///    - call the class (no-arg constructor) to create an instance
  /// 3. Cache and return the instance.
  ///
  /// @param iface Java interface type (e.g. {@code ForecastService.class})
  /// @param <T>   interface type parameter
  /// @return live Python instance as {@link Value}
  private <T> Value resolveInstance(Class<T> iface) {
    WeakReference<Value> ref = instanceCache.get(iface);
    Value cached = ref != null ? ref.get() : null;
    if (cached != null && !cached.isNull()) {
      return cached;
    }

    Source source = getFileSource(iface);
    context().eval(source);

    Value pyClass = resolveClass(iface);
    if (!pyClass.canExecute()) {
      throw new EvaluationException(
          "Python class '%s' is not callable".formatted(iface.getSimpleName()));
    }

    try {
      Value instance = pyClass.execute();
      instanceCache.put(iface, new WeakReference<>(instance));
      return instance;
    } catch (Exception e) {
      throw new EvaluationException(
          "Failed to instantiate Python class '%s': %s"
              .formatted(iface.getSimpleName(), e.getMessage()),
          e);
    }
  }

  /// ### resolveClass
  ///
  /// Locates the Python class corresponding to the given Java interface.
  ///
  /// Resolution order:
  /// 1. Polyglot bindings (for code using `polyglot.export_value("MyService", MyService)`)
  /// 2. Python language bindings (top-level class defined in the evaluated module)
  ///
  /// @param iface Java interface (e.g. {@code ForecastService.class})
  /// @param <T>   interface type
  /// @return Python class object as {@link Value}
  private <T> Value resolveClass(Class<T> iface) {
    String className = iface.getSimpleName();

    // 1) polyglot bindings
    Value polyglotBindings = context().getPolyglotBindings();
    Value exported = polyglotBindings.getMember(className);
    if (exported != null) {
      return exported;
    }

    // 2) Python language bindings
    Value pyBindings = context().getBindings(languageId());
    Value fromBindings = pyBindings.getMember(className);
    if (fromBindings != null) {
      return fromBindings;
    }

    throw new EvaluationException(
        "Python class '%s' not found in polyglot or language bindings".formatted(className));
  }

  /// ### invokeMember
  ///
  /// Invokes a method on a Python instance.
  ///
  /// @param target     Python instance
  /// @param methodName method name to invoke
  /// @param args       call arguments
  /// @return result as {@link Value}
  private Value invokeMember(Value target, String methodName, Object... args) {
    if (target == null || target.isNull()) {
      throw new EvaluationException(
          "Cannot invoke method '%s' on null Python instance".formatted(methodName));
    }

    Value member = target.getMember(methodName);
    if (member == null || !member.canExecute()) {
      throw new EvaluationException(
          "Python method '%s' not found or not executable".formatted(methodName));
    }

    try {
      return member.execute(args);
    } catch (Exception e) {
      throw new EvaluationException(
          "Error executing Python method '%s': %s".formatted(methodName, e.getMessage()), e);
    }
  }

  /// ### getFileSource
  ///
  /// Resolves the {@link Source} for the Python module associated with the given interface.
  ///
  /// Example:
  /// - interface: `ForecastService`
  /// - module name: `forecast_service`
  /// - file: `forecast_service.py`
  ///
  /// Uses {@link #sourceCache} to avoid rebuilding {@link Source} repeatedly.
  ///
  /// @param iface Java interface type
  /// @param <T>   interface type
  /// @return compiled {@link Source} for the module
  private <T> Source getFileSource(Class<T> iface) {
    return sourceCache.computeIfAbsent(
        iface,
        cls -> {
          String interfaceName = cls.getSimpleName();
          String moduleName = camelToSnake(interfaceName);
          return loadScript(SupportedLanguage.PYTHON, moduleName);
        });
  }
}
