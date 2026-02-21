package io.github.ih0rd.adapter.context;

import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.contract.ScriptSource;
import io.github.ih0rd.contract.SupportedLanguage;

/// # JsExecutor
///
/// JavaScript executor for GraalJS.
///
/// Responsibilities:
/// - Load JavaScript modules via {@link ScriptSource}
/// - Expose global JS functions as Java interface methods
/// - Validate bindings at startup
///
public final class JsExecutor extends AbstractPolyglotExecutor {

  /// ### JsExecutor
  ///
  /// @param context      GraalJS {@link Context}
  /// @param scriptSource script source abstraction
  public JsExecutor(Context context, ScriptSource scriptSource) {
    super(context, scriptSource);
  }

  @Override
  public String languageId() {
    return SupportedLanguage.JS.id();
  }

  /// ### evaluate(methodName, memberTargetType, args)
  ///
  /// Ensures the JS module for the given interface is loaded,
  /// then invokes a global function with the same name.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {

    ensureModuleLoaded(memberTargetType);
    return callFunction(methodName, args);
  }

  /// ### evaluate(methodName, memberTargetType)
  ///
  /// No-argument variant.
  @Override
  protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {

    ensureModuleLoaded(memberTargetType);
    return callFunction(methodName);
  }

  /// ### validateBinding
  ///
  /// Validates that:
  /// - the JavaScript module can be loaded
  /// - for each interface method there is a matching executable JS function
  @Override
  public <T> void validateBinding(Class<T> iface) {
    if (iface == null) {
      throw new IllegalArgumentException("Interface type must not be null");
    }

    ensureModuleLoaded(iface);

    Value bindings = context.getBindings(languageId());

    for (Method method : iface.getMethods()) {
      if (method.getDeclaringClass() == Object.class) {
        continue;
      }

      String name = method.getName();
      Value fn = bindings.getMember(name);

      if (fn == null || !fn.canExecute()) {
        throw new BindingException(
            "JavaScript function '%s' not found or not executable for interface '%s'"
                .formatted(name, iface.getName()));
      }
    }
  }

  /// ### metadata
  ///
  /// Extends base metadata with JS-specific details.
  @Override
  public Map<String, Object> metadata() {
    Map<String, Object> info = new LinkedHashMap<>(super.metadata());
    info.put("loadedInterfaces", sourceCache.keySet().stream().map(Class::getName).toList());
    return info;
  }

  /// ### create
  ///
  /// Creates a JS executor with a customized {@link Context.Builder}.
  ///
  /// NOTE:
  /// ScriptSource must be provided by the caller.
  public static JsExecutor create(ScriptSource scriptSource, Consumer<Context.Builder> customizer) {

    Context context = PolyglotHelper.newContext(SupportedLanguage.JS, customizer);
    return new JsExecutor(context, scriptSource);
  }

  /// ### createWithContext
  ///
  /// Creates a JS executor using a caller-provided {@link Context}.
  /// The caller is responsible for managing the context lifecycle.
  public static JsExecutor createWithContext(Context context, ScriptSource scriptSource) {

    return new JsExecutor(context, scriptSource);
  }

  /// ### ensureModuleLoaded
  ///
  /// Loads and evaluates the JS module associated with the given interface
  /// if it has not been loaded yet.
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
