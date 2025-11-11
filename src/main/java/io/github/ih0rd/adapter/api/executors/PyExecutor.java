package io.github.ih0rd.adapter.api.executors;

import static io.github.ih0rd.adapter.utils.CommonUtils.*;
import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import io.github.ih0rd.adapter.api.context.*;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.graalvm.polyglot.*;

/// # PyExecutor
///
/// GraalPy executor supporting:
/// - dynamic Java interface bindings
/// - inline Python code execution
/// - per-instance context safety (no static refs)
///
/// ---
/// ## Example
/// ```java
/// try (var py = PyExecutor.createDefault()) {
///     MyApi api = py.bind(MyApi.class);
///     var result = api.add(1, 2);
///     System.out.println(result);
/// }
/// ```
public final class PyExecutor extends BaseExecutor {

    /// ### INSTANCE_CACHE
    /// Stores Python class proxies per interface.
    /// Each instance belongs to its own GraalVM context.
    private final Map<Class<?>, Value> instanceCache = new ConcurrentHashMap<>();

    public PyExecutor(Context context, Path resourcesPath) {
        super(context, resourcesPath);
    }

    @Override
    public String languageId() {
        return SupportedLanguage.PYTHON.id();
    }

    /// ### createDefault
    /// Creates default Python executor with safe options.
    public static PyExecutor createDefault() {
        var ctx = PolyglotHelper.createPythonContext(true);
        var path = ResourcesProvider.get(SupportedLanguage.PYTHON);
        return new PyExecutor(ctx, path);
    }

    /// ### bind
    /// Dynamically binds a Java interface to a Python class implementation.
    /// ```java
    /// MyApi api = py.bind(MyApi.class);
    /// api.add(5, 10);
    /// ```
    @SuppressWarnings("unchecked")
    public <T> T bind(Class<T> iface) {
        var pyInstance = getOrCreateInstance(iface);
        return (T)
                Proxy.newProxyInstance(
                        iface.getClassLoader(),
                        new Class[] {iface},
                        (_, method, args) -> {
                            if (method.getDeclaringClass() == Object.class) {
                                return method.invoke(this, args);
                            }
                            return invokeMethod(iface, pyInstance, method.getName(), args).as(method.getReturnType());
                        });
    }

    /// ### getOrCreateInstance
    /// Loads Python class matching the Java interface name.
    private <T> Value getOrCreateInstance(Class<T> iface) {
        return instanceCache.computeIfAbsent(
                iface,
                cls -> {
                    var src = loadScript(SupportedLanguage.PYTHON, camelToSnake(cls.getSimpleName()));
                    context.eval(src);
                    var bindings = context.getPolyglotBindings();
                    var pyClass = getFirstElement(bindings.getMemberKeys());
                    if (!cls.getSimpleName().equals(pyClass)) {
                        throw new EvaluationException(
                                "Interface '%s' must match Python class '%s'"
                                        .formatted(cls.getSimpleName(), pyClass));
                    }
                    return bindings.getMember(pyClass).newInstance();
                });
    }

    /// ### invokeMethod
    /// Invokes Python method on a bound instance.
    private <T> Value invokeMethod(Class<T> iface, Value instance, String name, Object... args) {
        var member = instance.getMember(name);
        if (member == null || !member.canExecute()) {
            throw new EvaluationException("Method not found: " + name + " in " + iface.getSimpleName());
        }
        return member.execute(args);
    }

    /// ### evaluate
    /// Executes inline Python code directly.
    /// ```java
    /// var result = py.evaluate("1 + 2");
    /// System.out.println(result.asInt()); // 3
    /// ```
    public Value evaluate(String code) {
        try {
            return context.eval(Source.newBuilder(languageId(), code, "inline.py").buildLiteral());
        } catch (Exception e) {
            throw new EvaluationException("Error during Python eval", e);
        }
    }

    /// ### close
    /// Clears instance cache and closes GraalPy context.
    @Override
    public void close() {
        instanceCache.clear();
        super.close();
    }
}
