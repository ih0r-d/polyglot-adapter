package io.github.ih0rd.adapter.context;

import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.BindingException;

/// # JsExecutor
///
/// JavaScript executor for GraalJS (GraalVM 25).
///
/// Convention:
/// - Java interface: {@code MyService}
/// - JS file: {@code my_service.js}
/// - JS functions: {@code function someMethod(...) { ... }}
///
/// The executor:
/// - loads the matching JS module from classpath or filesystem
/// - exposes its global functions as method bindings for a Java interface
public final class JsExecutor extends AbstractPolyglotExecutor {

  /// ### JsExecutor
  ///
  /// @param context       GraalJS {@link Context}
  /// @param resourcesPath base path for javascript files
  public JsExecutor(Context context, Path resourcesPath) {
    super(context, resourcesPath);
  }

  @Override
  public String languageId() {
    return SupportedLanguage.JS.id();
  }

  /// ### evaluate(methodName, memberTargetType, args)
  ///
  /// Ensures the JS module for the given interface is loaded,
  /// then invokes a global function with the same name as the method.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
    ensureModuleLoaded(memberTargetType);
    return callFunction(methodName, args);
  }

  /// ### evaluate(methodName, memberTargetType)
  ///
  /// No-arg variant, same semantics as above.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
    ensureModuleLoaded(memberTargetType);
    return callFunction(methodName);
  }

  /// ### validateBinding
  ///
  /// For JS executor, validation ensures that:
  /// - the corresponding JS file can be loaded and evaluated
  /// - for each non-Object method in the interface there is a global JS function
  ///   with the same name, and it is executable.
  ///
  /// Any failure results in a {@link BindingException}.
  @Override
  public <T> void validateBinding(Class<T> iface) {
    if (iface == null) {
      throw new IllegalArgumentException("Interface type must not be null");
    }

    // Load & eval module if needed
    ensureModuleLoaded(iface);

    Value bindings = context.getBindings(languageId());

    for (Method m : iface.getMethods()) {
      if (m.getDeclaringClass() == Object.class) {
        continue;
      }
      String name = m.getName();
      Value fn = bindings.getMember(name);
      if (fn == null || !fn.canExecute()) {
        throw new BindingException(
            "JavaScript function '%s' not found or not executable for interface '%s'"
                .formatted(name, iface.getName()));
      }
    }
  }

  /// ### runtimeInfo
  ///
  /// Extends base metadata with JS-specific details.
  @Override
  public Map<String, Object> metadata() {
    Map<String, Object> info = new LinkedHashMap<>(super.metadata());
    info.put("loadedInterfaces", sourceCache.keySet().stream().map(Class::getName).toList());
    return info;
  }

  /// ### createDefault
  ///
  /// Creates a default JS executor with a standard GraalJS context.
  ///
  /// @return configured {@link JsExecutor} instance
  public static JsExecutor createDefault() {
    return createDefault(SupportedLanguage.JS, JsExecutor::new);
  }

  /// ### create
  ///
  /// Creates a JS executor with a customized {@link Context.Builder}.
  ///
  /// ```java
  /// JsExecutor exec = JsExecutor.create(builder ->
  ///     builder.option("engine.WarnInterpreterOnly", "false")
  /// );
  /// ```
  ///
  /// @param customizer builder customizer
  /// @return configured {@link JsExecutor} instance
  // context is closed by executor.close()
  public static JsExecutor create(Consumer<Context.Builder> customizer) {
    Path resourcesPath = ResourcesProvider.get(SupportedLanguage.JS);
    Context context = PolyglotHelper.newContext(SupportedLanguage.JS, customizer);
    return new JsExecutor(context, resourcesPath);
  }

  /// ### createWithContext
  ///
  /// Creates a JS executor that uses a caller-provided {@link Context}.
  /// The caller is responsible for managing the context lifecycle.
  public static JsExecutor createWithContext(Context context) {
    Path resourcesPath = ResourcesProvider.get(SupportedLanguage.JS);
    return new JsExecutor(context, resourcesPath);
  }

  /// ### ensureModuleLoaded
  ///
  /// Loads and evaluates the JS module associated with the given interface
  /// if it has not been loaded yet.
  ///
  /// Convention:
  /// - interface: {@code ForecastService}
  /// - module name: {@code forecast_service}
  /// - file: {@code forecast_service.js}
  private <T> void ensureModuleLoaded(Class<T> iface) {
    sourceCache.computeIfAbsent(
        iface,
        cls -> {
          String interfaceName = cls.getSimpleName();
          String moduleName = camelToSnake(interfaceName);
          Source src = loadScript(SupportedLanguage.JS, moduleName);
          context.eval(src);
          return src;
        });
  }
}
