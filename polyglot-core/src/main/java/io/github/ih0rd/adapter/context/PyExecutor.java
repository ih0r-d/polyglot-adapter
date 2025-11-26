package io.github.ih0rd.adapter.context;

import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.adapter.exceptions.InvocationException;

/// # PyExecutor
///
/// Python executor for GraalPy (GraalVM 25).
///
/// Convention:
/// - Java interface: {@code MyService}
/// - Python file: {@code my_service.py}
/// - Python class: {@code class MyService: ...}
///
/// The executor:
/// - loads the matching Python module from classpath or filesystem
/// - locates the Python class via polyglot or language bindings
/// - instantiates the class and caches the instance per interface type
public final class PyExecutor extends AbstractPolyglotExecutor {

  /// ### instanceCache
  /// Per-executor cache of Python instances keyed by Java interface type.
  ///
  /// Uses {@link WeakReference} to avoid keeping guest objects alive
  /// longer than the underlying context.
  private final Map<Class<?>, WeakReference<Value>> instanceCache = new ConcurrentHashMap<>();

  /// ### PyExecutor
  ///
  /// @param context       GraalPy {@link Context}
  /// @param resourcesPath base path for python scripts
  public PyExecutor(Context context, Path resourcesPath) {
    super(context, resourcesPath);
  }

  @Override
  public String languageId() {
    return SupportedLanguage.PYTHON.id();
  }

  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
    Value instance = resolveInstance(memberTargetType);
    return invokeMember(instance, methodName, args);
  }

  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
    Value instance = resolveInstance(memberTargetType);
    return invokeMember(instance, methodName);
  }

  /// ### validateBinding
  ///
  /// For Python executor, validation ensures that:
  /// - the corresponding Python module can be loaded
  /// - the Python class with the same name as the interface exists
  /// - the class is callable and can be instantiated
  ///
  /// Any failure results in an exception.
  @Override
  public <T> void validateBinding(Class<T> iface) {
    if (iface == null) {
      throw new IllegalArgumentException("Interface type must not be null");
    }
    // This will:
    // - load & eval the Python module
    // - resolve the Python class
    // - instantiate it (and cache the instance)
    resolveInstance(iface);
  }

  /// ### metadata
  ///
  /// Extends base metadata with Python-specific details.
  @Override
  public Map<String, Object> metadata() {
    Map<String, Object> info = new LinkedHashMap<>(super.metadata());
    info.put("cachedInterfaces", instanceCache.keySet().stream().map(Class::getName).toList());
    info.put("instanceCacheSize", instanceCache.size());
    return info;
  }

  /// ### createDefault
  ///
  /// Creates a default Python executor with a standard GraalPy context.
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
  /// PyExecutor exec = PyExecutor.create(builder ->
  ///     builder.option("engine.WarnInterpreterOnly", "false")
  /// );
  /// ```
  ///
  /// @param customizer builder customizer
  /// @return configured {@link PyExecutor} instance
  @SuppressWarnings("resource") // context is closed by executor.close()
  public static PyExecutor create(Consumer<Context.Builder> customizer) {
    Path resourcesPath = ResourcesProvider.get(SupportedLanguage.PYTHON);
    Context context = PolyglotHelper.newContext(SupportedLanguage.PYTHON, customizer);
    return new PyExecutor(context, resourcesPath);
  }

  /// ### createWithContext
  ///
  /// Creates a Python executor that uses a caller-provided {@link Context}.
  /// The caller is responsible for managing the context lifecycle.
  public static PyExecutor createWithContext(Context context) {
    Path resourcesPath = ResourcesProvider.get(SupportedLanguage.PYTHON);
    return new PyExecutor(context, resourcesPath);
  }

  /// ### resolveInstance
  ///
  /// Resolves or creates a Python instance for a given Java interface type.
  ///
  /// Steps:
  /// 1. Lookup cached instance in {@link #instanceCache}
  /// 2. If missing:
  ///    - load and eval script for the interface (e.g. {@code my_service.py})
  ///    - locate the Python class (via polyglot or language bindings)
  ///    - call the class to create an instance
  /// 3. Cache and return the instance.
  private <T> Value resolveInstance(Class<T> iface) {
    WeakReference<Value> ref = instanceCache.get(iface);
    Value cached = ref != null ? ref.get() : null;
    if (cached != null && !cached.isNull()) {
      return cached;
    }

    Source source = getFileSource(iface);
    context.eval(source);

    Value pyClass = resolveClass(iface);
    if (!pyClass.canExecute()) {
      throw new BindingException(
          "Python class '%s' is not callable".formatted(iface.getSimpleName()));
    }

    try {
      Value instance = pyClass.execute();
      instanceCache.put(iface, new WeakReference<>(instance));
      return instance;
    } catch (Exception e) {
      throw new InvocationException(
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
  /// 1. polyglot bindings (for code using {@code polyglot.export_value})
  /// 2. language bindings (for classes defined directly in the module)
  private <T> Value resolveClass(Class<T> iface) {
    String className = iface.getSimpleName();

    // 1) polyglot bindings (e.g. via polyglot.export_value("MyService", MyService))
    Value polyglotBindings = context.getPolyglotBindings();
    Value exported = polyglotBindings.getMember(className);
    if (exported != null) {
      return exported;
    }

    // 2) Python language bindings (top-level class in the evaluated module)
    Value pyBindings = context.getBindings(languageId());
    Value fromBindings = pyBindings.getMember(className);
    if (fromBindings != null) {
      return fromBindings;
    }

    throw new BindingException(
        "Python class '%s' not found in polyglot or language bindings".formatted(className));
  }

  /// ### invokeMember
  ///
  /// Invokes a method on a Python instance.
  ///
  /// @param target     Python object
  /// @param methodName method name
  /// @param args       call arguments
  /// @return result as {@link Value}
  private Value invokeMember(Value target, String methodName, Object... args) {
    if (target == null || target.isNull()) {
      throw new BindingException(
          "Cannot invoke method '%s' on null Python instance".formatted(methodName));
    }

    Value member = target.getMember(methodName);
    if (member == null || !member.canExecute()) {
      throw new BindingException(
          "Python method '%s' not found or not executable".formatted(methodName));
    }

    try {
      return member.execute(args);
    } catch (Exception e) {
      throw new InvocationException(
          "Error executing Python method '%s': %s".formatted(methodName, e.getMessage()), e);
    }
  }

  /// ### getFileSource
  ///
  /// Resolves the {@link Source} for the Python module associated with the given interface.
  ///
  /// Example:
  /// - interface: {@code ForecastService}
  /// - module name: {@code forecast_service}
  /// - file: {@code forecast_service.py}
  private <T> Source getFileSource(Class<T> iface) {
    return sourceCache.computeIfAbsent(
        iface,
        cls -> {
          String interfaceName = cls.getSimpleName();
          String moduleName = camelToSnake(interfaceName);
          return loadScript(SupportedLanguage.PYTHON, moduleName);
        });
  }

  /// ### clearInstanceCache
  ///
  /// Clears the cached Python instances.
  public void clearInstanceCache() {
    instanceCache.clear();
  }

  /// ### clearAllCaches
  ///
  /// Clears all caches maintained by this executor (instances + sources).
  @Override
  public void clearAllCaches() {
    clearInstanceCache();
    super.clearAllCaches();
  }
}
