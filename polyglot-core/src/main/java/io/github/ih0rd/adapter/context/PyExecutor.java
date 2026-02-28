package io.github.ih0rd.adapter.context;

import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.adapter.exceptions.InvocationException;
import io.github.ih0rd.contract.ScriptSource;
import io.github.ih0rd.contract.SupportedLanguage;

/// # PyExecutor
///
/// Python executor for GraalPy.
///
/// Responsibilities:
/// - Load Python modules via {@link ScriptSource}
/// - Resolve Python classes matching Java interfaces
/// - Instantiate and cache Python objects per interface
/// - Invoke Python methods via polyglot interop
///
public final class PyExecutor extends AbstractPolyglotExecutor {

  /// ### instanceCache
  /// Cache of Python instances keyed by Java interface type.
  ///
  /// Uses {@link WeakReference} to avoid retaining guest objects
  /// beyond the lifetime of the underlying context.
  private final Map<Class<?>, WeakReference<Value>> instanceCache = new ConcurrentHashMap<>();

  /// ### PyExecutor
  ///
  /// @param context       GraalPy {@link Context}
  /// @param scriptSource script source abstraction
  public PyExecutor(Context context, ScriptSource scriptSource) {
    super(context, scriptSource);
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
  /// Validates that the Python module and class corresponding
  /// to the given Java interface can be resolved and instantiated.
  @Override
  public <T> void validateBinding(Class<T> iface) {
    if (iface == null) {
      throw new IllegalArgumentException("Interface type must not be null");
    }
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

  /// ### create
  ///
  /// Creates a Python executor with a customized {@link Context.Builder}.
  ///
  /// NOTE:
  /// ScriptSource must be provided by the caller.
  ///
  /// @param scriptSource script source implementation
  /// @param customizer   context builder customizer
  /// @return configured {@link PyExecutor}
  // context is closed by executor.close()
  public static PyExecutor create(ScriptSource scriptSource, Consumer<Context.Builder> customizer) {

    Context context = PolyglotHelper.newContext(SupportedLanguage.PYTHON, customizer);
    return new PyExecutor(context, scriptSource);
  }

  /// ### createWithContext
  ///
  /// Creates a Python executor using a caller-provided {@link Context}.
  ///
  /// The caller is responsible for managing the context lifecycle.
  public static PyExecutor createWithContext(Context context, ScriptSource scriptSource) {

    return new PyExecutor(context, scriptSource);
  }

  /// ### resolveInstance
  ///
  /// Resolves or creates a Python target object for the given Java interface.
  ///
  /// Supported export styles:
  ///
  /// Class-style export
  ///    The exported value is a callable Python class.
  ///    The executor instantiates it via execute().
  ///
  ///    Example:
  ///    class StatsApi:
  ///        ...
  ///    polyglot.export_value("StatsApi", StatsApi)
  ///
  /// Object-style export (function map)
  ///    The exported value is a dictionary-like object containing functions.
  ///    The executor uses it directly without instantiation.
  ///
  ///    Example:
  ///    polyglot.export_value(
  ///        "StatsApiV2",
  ///        {
  ///            "randomNumbers": randomNumbers,
  ///            "stats": stats
  ///        }
  ///    )
  ///
  /// The resolved target is cached per interface using WeakReference
  /// to avoid retaining guest objects longer than the context lifecycle.
  ///
  /// @param iface Java interface type
  /// @return resolved Python instance or exported object
  private <T> Value resolveInstance(Class<T> iface) {

    WeakReference<Value> ref = instanceCache.get(iface);
    Value cached = (ref != null ? ref.get() : null);
    if (cached != null && !cached.isNull()) {
      return cached;
    }

    Source source = resolveSource(iface);
    context.eval(source);

    Value exported = resolveClass(iface);

    Value instance;

    if (exported.canExecute()) {
      // class-style export
      try {
        instance = exported.execute();
      } catch (Exception e) {
        throw new InvocationException(
            "Failed to instantiate Python class '%s'".formatted(iface.getSimpleName()), e);
      }
    } else {
      // object-style export (map of functions)
      instance = exported;
    }

    instanceCache.put(iface, new WeakReference<>(instance));
    return instance;
  }

  /// ### resolveClass
  ///
  /// Locates the Python class corresponding to the given Java interface.
  private <T> Value resolveClass(Class<T> iface) {
    String className = iface.getSimpleName();

    Value polyglotBindings = context.getPolyglotBindings();
    Value exported = polyglotBindings.getMember(className);
    if (exported != null) {
      return exported;
    }

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
  /// Invokes a method on a resolved Python target.
  ///
  /// Supports two export styles:
  ///
  /// Class-style export
  ///    Methods are exposed as object members and accessed via getMember().
  ///
  /// Object-style export (dictionary of functions)
  ///    Methods are stored as hash entries and accessed via getHashValue().
  ///
  /// The method resolves the correct access strategy automatically.
  ///
  /// @param target resolved Python instance or exported object
  /// @param methodName method name to invoke
  /// @param args arguments passed to the Python function
  /// @return result of the Python invocation
  private Value invokeMember(Value target, String methodName, Object... args) {

    if (target == null || target.isNull()) {
      throw new BindingException(
          "Cannot invoke method '%s' on null Python target".formatted(methodName));
    }

    Value member = null;

    // Class-style: method exposed as member
    if (target.hasMember(methodName)) {
      member = target.getMember(methodName);
    }
    // Object-style: exported dict of functions
    else if (target.hasHashEntries()) {
      member = target.getHashValue(methodName);
    }

    if (member == null || !member.canExecute()) {
      throw new BindingException(
          "Python method '%s' not found or not executable".formatted(methodName));
    }

    try {
      return member.execute(args);
    } catch (Exception e) {
      throw new InvocationException("Error executing Python method '%s'".formatted(methodName), e);
    }
  }

  /// ### resolveSource
  ///
  /// Resolves and caches the {@link Source} for the Python module
  /// associated with the given Java interface.
  private <T> Source resolveSource(Class<T> iface) {
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
  /// Clears cached Python instances.
  public void clearInstanceCache() {
    instanceCache.clear();
  }

  /// ### clearAllCaches
  ///
  /// Clears all executor caches.
  @Override
  public void clearAllCaches() {
    clearInstanceCache();
    super.clearAllCaches();
  }
}
