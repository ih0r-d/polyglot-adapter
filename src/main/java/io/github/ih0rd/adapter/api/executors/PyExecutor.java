package io.github.ih0rd.adapter.api.executors;

import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import io.github.ih0rd.adapter.api.context.ResourcesProvider;
import io.github.ih0rd.adapter.api.context.SupportedLanguage;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

public final class PyExecutor extends BaseExecutor {

  private final Map<String, WeakReference<Value>> instanceCache = new ConcurrentHashMap<>();

  public PyExecutor(Context context) {
    super(context, ResourcesProvider.get(SupportedLanguage.PYTHON));
  }

  @Override
  public String languageId() {
    return SupportedLanguage.PYTHON.id();
  }

  @SuppressWarnings("unchecked")
  public <T> T bind(Class<T> iface) {
    var pyInstance = getOrCreateInstance(iface);
    return (T)
        Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[] {iface},
            (_, method, args) -> {
              if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
              }
              return invokeMethod(iface, pyInstance, method.getName(), args)
                  .as(method.getReturnType());
            });
  }

  private <T> Value getOrCreateInstance(Class<T> iface) {
    String pyClass = iface.getSimpleName();
    WeakReference<Value> ref = instanceCache.get(pyClass);
    if (ref != null) {
      Value cached = ref.get();
      if (cached != null) {
        return cached;
      }
    }

    Value instance = createPythonInstance(iface, pyClass);
    instanceCache.put(pyClass, new WeakReference<>(instance));
    return instance;
  }

  private <T> Value createPythonInstance(Class<T> iface, String pyClass) {
    var bindings = context.getPolyglotBindings();

    if (!bindings.hasMember(pyClass)) {
      var src = loadScript(SupportedLanguage.PYTHON, camelToSnake(pyClass));
      try {
        context.eval(src);
      } catch (Exception e) {
        throw new EvaluationException(
            "Failed to load Python script for class '" + pyClass + "'", e);
      }
      bindings = context.getPolyglotBindings();
    }

    if (!bindings.hasMember(pyClass)) {
      throw new EvaluationException(
          "Python class '" + pyClass + "' not found for interface '" + iface.getName() + "'");
    }

    return bindings.getMember(pyClass).newInstance();
  }

  private <T> Value invokeMethod(Class<T> iface, Value instance, String name, Object... args) {
    var member = instance.getMember(name);
    if (member == null || !member.canExecute()) {
      throw new EvaluationException("Method not found: " + name + " in " + iface.getSimpleName());
    }
    return member.execute(args);
  }

  public Value evaluate(String code) {
    try {
      Source source = Source.newBuilder(languageId(), code, "inline.py").build();
      return context.eval(source);
    } catch (Exception e) {
      throw new EvaluationException("Error during Python eval", e);
    }
  }

  @Override
  public void close() {
    instanceCache.clear();
    super.close();
  }
}
