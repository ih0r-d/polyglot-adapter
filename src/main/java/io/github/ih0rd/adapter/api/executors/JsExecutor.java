package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.*;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.graalvm.polyglot.*;

/// # JsExecutor
///
/// GraalJS executor providing:
/// - dynamic JS interface bindings
/// - inline JavaScript evaluation
/// - Node.js mode support
///
/// ---
/// ## Example
/// ```java
/// try (var js = JsExecutor.createDefault()) {
///     MyJsApi api = js.bind(MyJsApi.class);
///     var res = api.add(10, 5);
///     System.out.println(res);
/// }
/// ```
public final class JsExecutor extends BaseExecutor {

    /// ### JS_SOURCE_CACHE
    /// Per-instance cache of loaded JavaScript sources.
    private final Map<Class<?>, Source> jsSourceCache = new ConcurrentHashMap<>();

    public JsExecutor(Context context, Path resourcesPath) {
        super(context, resourcesPath);
    }

    /// ### createDefault
    /// Creates a standard GraalJS context (no Node.js support).
    public static JsExecutor createDefault() {
        var ctx = PolyglotHelper.createJsContext(false);
        var path = ResourcesProvider.get(SupportedLanguage.JS);
        return new JsExecutor(ctx, path);
    }

    /// ### createWithNode
    /// Creates a GraalJS context with Node.js features enabled.
    public static JsExecutor createWithNode() {
        var ctx = PolyglotHelper.createJsContext(true);
        var path = ResourcesProvider.get(SupportedLanguage.JS);
        return new JsExecutor(ctx, path);
    }

    @Override
    public String languageId() {
        return SupportedLanguage.JS.id();
    }

    /// ### bind
    /// Dynamically binds Java interface methods to JS functions.
    @SuppressWarnings("unchecked")
    public <T> T bind(Class<T> iface) {
        var src = getOrLoadSource(iface);
        context.eval(src);
        return (T)
                Proxy.newProxyInstance(
                        iface.getClassLoader(),
                        new Class[] {iface},
                        (_, method, args) -> {
                            if (method.getDeclaringClass() == Object.class) {
                                return method.invoke(this, args);
                            }
                            var result = callFunction(method.getName(), args);
                            return result.isNull() ? null : result.as(method.getReturnType());
                        });
    }

    /// ### getOrLoadSource
    /// Loads JS source file corresponding to the interface name.
    private <T> Source getOrLoadSource(Class<T> iface) {
        return jsSourceCache.computeIfAbsent(
                iface, cls -> loadScript(SupportedLanguage.JS, cls.getSimpleName()));
    }

    /// ### evaluate
    /// Executes inline JS code.
    /// ```java
    /// var value = js.evaluate("1 + 2");
    /// System.out.println(value.asInt()); // 3
    /// ```
    public Value evaluate(String code) {
        try {
            return context.eval(Source.newBuilder(languageId(), code, "inline.js").buildLiteral());
        } catch (Exception e) {
            throw new EvaluationException("Error during JS eval", e);
        }
    }

    /// ### close
    /// Clears caches and closes the GraalJS context.
    @Override
    public void close() {
        jsSourceCache.clear();
        super.close();
    }
}
