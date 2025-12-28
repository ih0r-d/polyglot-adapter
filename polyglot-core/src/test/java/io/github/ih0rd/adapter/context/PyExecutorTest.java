package io.github.ih0rd.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.adapter.exceptions.InvocationException;
import io.github.ih0rd.adapter.script.ScriptSource;

@SuppressWarnings({"unchecked"})
class PyExecutorTest {

  interface Api {
    String hello(String arg);
  }

  private ScriptSource mockScriptSource() throws Exception {
    ScriptSource ss = mock(ScriptSource.class);
    when(ss.exists(eq(SupportedLanguage.PYTHON), any())).thenReturn(true);

    Source src = mock(Source.class);
    when(ss.open(eq(SupportedLanguage.PYTHON), any())).thenReturn(mock(java.io.Reader.class));

    return ss;
  }

  private PyExecutor newExec(Context ctx) throws Exception {
    return new PyExecutor(ctx, mockScriptSource());
  }

  private void callResolveInstance(PyExecutor exec) {
    try {
      Method m = PyExecutor.class.getDeclaredMethod("resolveInstance", Class.class);
      m.setAccessible(true);
      m.invoke(exec, (Class<?>) Api.class);
    } catch (Exception e) {
      Throwable c = e.getCause();
      if (c instanceof RuntimeException r) throw r;
      throw new RuntimeException(e);
    }
  }

  private Value callResolveClass(PyExecutor exec, Class<?> iface) {
    try {
      Method m = PyExecutor.class.getDeclaredMethod("resolveClass", Class.class);
      m.setAccessible(true);
      return (Value) m.invoke(exec, iface);
    } catch (Exception e) {
      Throwable c = e.getCause();
      if (c instanceof RuntimeException r) throw r;
      throw new RuntimeException(e);
    }
  }

  private Value callInvokeMember(PyExecutor exec, Value target, String name, Object... args) {
    try {
      Method m =
          PyExecutor.class.getDeclaredMethod(
              "invokeMember", Value.class, String.class, Object[].class);
      m.setAccessible(true);
      return (Value) m.invoke(exec, target, name, args);
    } catch (Exception e) {
      Throwable c = e.getCause();
      if (c instanceof RuntimeException r) throw r;
      throw new RuntimeException(e);
    }
  }

  @Test
  void languageId_returnsPython() throws Exception {
    PyExecutor exec = newExec(mock(Context.class));
    assertEquals("python", exec.languageId());
  }

  @Test
  void evaluateWithArgs_usesCachedInstance() throws Exception {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    Value instance = mock(Value.class);
    Value member = mock(Value.class);
    Value result = mock(Value.class);

    when(instance.isNull()).thenReturn(false);
    when(instance.getMember("hello")).thenReturn(member);
    when(member.canExecute()).thenReturn(true);
    when(member.execute("x")).thenReturn(result);

    Field f = PyExecutor.class.getDeclaredField("instanceCache");
    f.setAccessible(true);
    Map<Class<?>, WeakReference<Value>> cache = (Map<Class<?>, WeakReference<Value>>) f.get(exec);
    cache.put(Api.class, new WeakReference<>(instance));

    Value out = exec.evaluate("hello", Api.class, "x");
    assertSame(result, out);
  }

  @Test
  void resolveInstance_notCallable_throws() throws Exception {
    Context ctx = mock(Context.class);
    PyExecutor exec = spy(newExec(ctx));

    doReturn(mock(Source.class)).when(exec).loadScript(eq(SupportedLanguage.PYTHON), any());

    when(ctx.eval(any(Source.class))).thenReturn(mock(Value.class));

    Value poly = mock(Value.class);
    Value pyClass = mock(Value.class);
    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(pyClass);
    when(pyClass.canExecute()).thenReturn(false);

    assertThrows(BindingException.class, () -> callResolveInstance(exec));
  }

  @Test
  void resolveInstance_executeThrows_wrapped() throws Exception {
    Context ctx = mock(Context.class);
    PyExecutor exec = spy(newExec(ctx));

    doReturn(mock(Source.class)).when(exec).loadScript(eq(SupportedLanguage.PYTHON), any());

    when(ctx.eval(any(Source.class))).thenReturn(mock(Value.class));

    Value poly = mock(Value.class);
    Value pyClass = mock(Value.class);
    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(pyClass);
    when(pyClass.canExecute()).thenReturn(true);
    when(pyClass.execute()).thenThrow(new RuntimeException("boom"));

    assertThrows(InvocationException.class, () -> callResolveInstance(exec));
  }

  @Test
  void resolveClass_fromPolyglotBindings() throws Exception {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    Value poly = mock(Value.class);
    Value cls = mock(Value.class);

    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(cls);

    assertSame(cls, callResolveClass(exec, Api.class));
  }

  @Test
  void resolveClass_notFound_throws() throws Exception {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    Value poly = mock(Value.class);
    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(null);

    Value lang = mock(Value.class);
    when(ctx.getBindings("python")).thenReturn(lang);
    when(lang.getMember("Api")).thenReturn(null);

    assertThrows(BindingException.class, () -> callResolveClass(exec, Api.class));
  }

  @Test
  void invokeMember_executeThrows_wrapped() throws Exception {
    PyExecutor exec = newExec(mock(Context.class));
    Value target = mock(Value.class);
    Value member = mock(Value.class);

    when(target.isNull()).thenReturn(false);
    when(target.getMember("hello")).thenReturn(member);
    when(member.canExecute()).thenReturn(true);
    when(member.execute(any())).thenThrow(new RuntimeException("err"));

    assertThrows(InvocationException.class, () -> callInvokeMember(exec, target, "hello", "x"));
  }
}
