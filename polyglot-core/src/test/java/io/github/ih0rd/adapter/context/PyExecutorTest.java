package io.github.ih0rd.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.github.ih0rd.adapter.exceptions.EvaluationException;

@SuppressWarnings({"unchecked", "resource"})
class PyExecutorTest {

  interface Api {
    String hello(String arg);
  }

  private PyExecutor newExec(Context ctx) {
    return new PyExecutor(ctx, Path.of("/tmp"));
  }

  private Value callResolveInstance(PyExecutor exec, Class<?> iface) {
    try {
      Method m = PyExecutor.class.getDeclaredMethod("resolveInstance", Class.class);
      m.setAccessible(true);
      return (Value) m.invoke(exec, iface);
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

  private Source callGetFileSource(PyExecutor exec, Class<?> iface) {
    try {
      Method m = PyExecutor.class.getDeclaredMethod("getFileSource", Class.class);
      m.setAccessible(true);
      return (Source) m.invoke(exec, iface);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void languageId_returnsPython() {
    PyExecutor exec = newExec(mock(Context.class));
    assertEquals("python", exec.languageId());
  }

  @Test
  void evaluateWithArgs_usesCachedInstanceAndInvokeMember() throws Exception {
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
    @SuppressWarnings("unchecked")
    Map<Class<?>, WeakReference<Value>> cache = (Map<Class<?>, WeakReference<Value>>) f.get(exec);
    cache.put(Api.class, new WeakReference<>(instance));

    Value out = exec.evaluate("hello", Api.class, "x");
    assertSame(result, out);
  }

  @Test
  void evaluateNoArgs_usesCachedInstance() throws Exception {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    interface NoArgApi {
      String ping();
    }

    Value instance = mock(Value.class);
    Value member = mock(Value.class);
    Value result = mock(Value.class);

    when(instance.isNull()).thenReturn(false);
    when(instance.getMember("ping")).thenReturn(member);
    when(member.canExecute()).thenReturn(true);
    when(member.execute()).thenReturn(result);

    Field f = PyExecutor.class.getDeclaredField("instanceCache");
    f.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Class<?>, WeakReference<Value>> cache = (Map<Class<?>, WeakReference<Value>>) f.get(exec);
    cache.put(NoArgApi.class, new WeakReference<>(instance));

    Value out = exec.evaluate("ping", NoArgApi.class);
    assertSame(result, out);
  }

  @Test
  void resolveInstance_createsAndCaches() {
    Context ctx = mock(Context.class);
    PyExecutor exec = spy(newExec(ctx));

    Source src = mock(Source.class);
    doReturn(src).when(exec).loadScript(eq(SupportedLanguage.PYTHON), eq("api"));

    when(ctx.eval(any(Source.class))).thenReturn(mock(Value.class));

    Value poly = mock(Value.class);
    Value pyClass = mock(Value.class);
    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(pyClass);

    when(pyClass.canExecute()).thenReturn(true);
    Value instance = mock(Value.class);
    when(pyClass.execute()).thenReturn(instance);

    Value r1 = callResolveInstance(exec, Api.class);
    Value r2 = callResolveInstance(exec, Api.class);

    assertSame(instance, r1);
    assertSame(r1, r2);
    verify(pyClass, atLeastOnce()).execute();
  }

  @Test
  void resolveInstance_notCallable_throws() {
    Context ctx = mock(Context.class);
    PyExecutor exec = spy(newExec(ctx));

    Source src = mock(Source.class);
    doReturn(src).when(exec).loadScript(eq(SupportedLanguage.PYTHON), eq("api"));

    when(ctx.eval(any(Source.class))).thenReturn(mock(Value.class));

    Value poly = mock(Value.class);
    Value pyClass = mock(Value.class);

    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(pyClass);

    when(pyClass.canExecute()).thenReturn(false);

    assertThrows(EvaluationException.class, () -> callResolveInstance(exec, Api.class));
  }

  @Test
  void resolveInstance_executeThrows_wrapped() {
    Context ctx = mock(Context.class);
    PyExecutor exec = spy(newExec(ctx));

    Source src = mock(Source.class);
    doReturn(src).when(exec).loadScript(eq(SupportedLanguage.PYTHON), eq("api"));
    when(ctx.eval(any(Source.class))).thenReturn(mock(Value.class));

    Value poly = mock(Value.class);
    Value pyClass = mock(Value.class);
    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(pyClass);

    when(pyClass.canExecute()).thenReturn(true);
    when(pyClass.execute()).thenThrow(new RuntimeException("boom"));

    assertThrows(EvaluationException.class, () -> callResolveInstance(exec, Api.class));
  }

  @Test
  void resolveClass_fromPolyglotBindings() {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    Value poly = mock(Value.class);
    Value cls = mock(Value.class);

    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(cls);

    assertSame(cls, callResolveClass(exec, Api.class));
  }

  @Test
  void resolveClass_fromLanguageBindings() {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    Value poly = mock(Value.class);
    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(null);

    Value lang = mock(Value.class);
    Value cls = mock(Value.class);
    when(ctx.getBindings("python")).thenReturn(lang);
    when(lang.getMember("Api")).thenReturn(cls);

    assertSame(cls, callResolveClass(exec, Api.class));
  }

  @Test
  void resolveClass_notFound_throws() {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    Value poly = mock(Value.class);
    when(ctx.getPolyglotBindings()).thenReturn(poly);
    when(poly.getMember("Api")).thenReturn(null);

    Value lang = mock(Value.class);
    when(ctx.getBindings("python")).thenReturn(lang);
    when(lang.getMember("Api")).thenReturn(null);

    assertThrows(EvaluationException.class, () -> callResolveClass(exec, Api.class));
  }

  @Test
  void invokeMember_ok() {
    Context ctx = mock(Context.class);
    PyExecutor exec = newExec(ctx);

    Value target = mock(Value.class);
    Value member = mock(Value.class);
    Value result = mock(Value.class);

    when(target.isNull()).thenReturn(false);
    when(target.getMember("hello")).thenReturn(member);
    when(member.canExecute()).thenReturn(true);
    when(member.execute("x")).thenReturn(result);

    Value out = callInvokeMember(exec, target, "hello", "x");
    assertSame(result, out);
  }

  @Test
  void invokeMember_nullTarget_throws() {
    PyExecutor exec = newExec(mock(Context.class));
    Value target = mock(Value.class);
    when(target.isNull()).thenReturn(true);

    assertThrows(EvaluationException.class, () -> callInvokeMember(exec, target, "hello", "x"));
  }

  @Test
  void invokeMember_missingMethod_throws() {
    PyExecutor exec = newExec(mock(Context.class));
    Value target = mock(Value.class);

    when(target.isNull()).thenReturn(false);
    when(target.getMember("hello")).thenReturn(null);

    assertThrows(EvaluationException.class, () -> callInvokeMember(exec, target, "hello", "x"));
  }

  @Test
  void invokeMember_notExecutable_throws() {
    PyExecutor exec = newExec(mock(Context.class));
    Value target = mock(Value.class);
    Value member = mock(Value.class);

    when(target.isNull()).thenReturn(false);
    when(target.getMember("hello")).thenReturn(member);
    when(member.canExecute()).thenReturn(false);

    assertThrows(EvaluationException.class, () -> callInvokeMember(exec, target, "hello", "x"));
  }

  @Test
  void invokeMember_executeThrows_wrapped() {
    PyExecutor exec = newExec(mock(Context.class));
    Value target = mock(Value.class);
    Value member = mock(Value.class);

    when(target.isNull()).thenReturn(false);
    when(target.getMember("hello")).thenReturn(member);
    when(member.canExecute()).thenReturn(true);
    when(member.execute(any())).thenThrow(new RuntimeException("err"));

    assertThrows(EvaluationException.class, () -> callInvokeMember(exec, target, "hello", "x"));
  }

  @Test
  void getFileSource_usesCacheAndSnakeCase() {
    Context ctx = mock(Context.class);
    PyExecutor exec = spy(newExec(ctx));

    Source src = mock(Source.class);
    doReturn(src).when(exec).loadScript(SupportedLanguage.PYTHON, "api");

    Source first = callGetFileSource(exec, Api.class);
    Source second = callGetFileSource(exec, Api.class);

    assertSame(first, second);
    verify(exec, times(1)).loadScript(SupportedLanguage.PYTHON, "api");
  }

  @Test
  void createDefault_usesBaseFactory() {
    Path rp = Path.of("/py");
    Context ctx = mock(Context.class);

    try (MockedStatic<ResourcesProvider> res = mockStatic(ResourcesProvider.class);
        MockedStatic<PolyglotHelper> helper = mockStatic(PolyglotHelper.class)) {

      res.when(() -> ResourcesProvider.get(SupportedLanguage.PYTHON)).thenReturn(rp);
      helper.when(() -> PolyglotHelper.newContext(SupportedLanguage.PYTHON)).thenReturn(ctx);

      PyExecutor exec = PyExecutor.createDefault();
      assertNotNull(exec);
    }
  }

  @Test
  void createWithCustomizer_usesHelper() {
    Path rp = Path.of("/py");
    Context ctx = mock(Context.class);

    try (MockedStatic<ResourcesProvider> res = mockStatic(ResourcesProvider.class);
        MockedStatic<PolyglotHelper> helper = mockStatic(PolyglotHelper.class)) {

      res.when(() -> ResourcesProvider.get(SupportedLanguage.PYTHON)).thenReturn(rp);
      helper
          .when(() -> PolyglotHelper.newContext(eq(SupportedLanguage.PYTHON), any(Consumer.class)))
          .thenReturn(ctx);

      PyExecutor exec = PyExecutor.create(b -> b.option("python.VerboseFlag", "false"));
      assertNotNull(exec);
    }
  }
}
