package io.github.ih0rd.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.StringReader;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.adapter.script.ScriptSource;

class JsExecutorTest {

  interface MyJsApi {
    void doSomething();
  }

  @Test
  void languageId_returnsJs() {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);
    JsExecutor executor = new JsExecutor(ctx, ss);

    assertEquals("js", executor.languageId());
  }

  @Test
  void validateBinding_nullInterface_throws() {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);
    JsExecutor executor = new JsExecutor(ctx, ss);

    assertThrows(IllegalArgumentException.class, () -> executor.validateBinding(null));
  }

  @Test
  void validateBinding_success() throws Exception {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);

    // Mock ScriptSource
    when(ss.exists(eq(SupportedLanguage.JS), any())).thenReturn(true);
    when(ss.open(eq(SupportedLanguage.JS), any())).thenReturn(new StringReader(""));

    // Mock Context
    Value bindings = mock(Value.class);
    when(ctx.getBindings("js")).thenReturn(bindings);

    // Mock function check
    Value fn = mock(Value.class);
    when(bindings.getMember("doSomething")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);

    JsExecutor executor = new JsExecutor(ctx, ss);
    executor.validateBinding(MyJsApi.class);

    verify(ctx).eval(any(Source.class));
  }

  @Test
  void validateBinding_functionNotFound_throws() throws Exception {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);

    when(ss.exists(any(), any())).thenReturn(true);
    when(ss.open(any(), any())).thenReturn(new StringReader(""));

    Value bindings = mock(Value.class);
    when(ctx.getBindings("js")).thenReturn(bindings);

    // Function missing
    when(bindings.getMember("doSomething")).thenReturn(null);

    JsExecutor executor = new JsExecutor(ctx, ss);

    assertThrows(BindingException.class, () -> executor.validateBinding(MyJsApi.class));
  }

  @Test
  void evaluate_loadsModuleAndCallsFunction() throws Exception {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);

    when(ss.exists(any(), any())).thenReturn(true);
    when(ss.open(any(), any())).thenReturn(new StringReader(""));

    Value bindings = mock(Value.class);
    when(ctx.getBindings("js")).thenReturn(bindings);

    Value fn = mock(Value.class);
    when(bindings.getMember("doSomething")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);
    when(fn.execute(any())).thenReturn(Value.asValue(true)); // Mock return

    JsExecutor executor = new JsExecutor(ctx, ss);

    // Call evaluate
    executor.evaluate("doSomething", MyJsApi.class);

    verify(fn).execute(); // executed with no args
    verify(ctx).eval(any(Source.class)); // module loaded
  }

  @Test
  void metadata_includesLoadedInterfaces() throws Exception {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);

    when(ss.exists(any(), any())).thenReturn(true);
    when(ss.open(any(), any())).thenReturn(new StringReader(""));

    Value bindings = mock(Value.class);
    when(ctx.getBindings("js")).thenReturn(bindings);
    Value fn = mock(Value.class);
    when(bindings.getMember("doSomething")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);

    JsExecutor executor = new JsExecutor(ctx, ss);
    // Since ensureModuleLoaded is private, we can trigger it via validateBinding or evaluate
    executor.validateBinding(MyJsApi.class);

    Map<String, Object> meta = executor.metadata();
    assertTrue(meta.containsKey("loadedInterfaces"));
    assertTrue(meta.get("loadedInterfaces").toString().contains("MyJsApi"));
  }

  @Test
  void create_factories() {
    ScriptSource ss = mock(ScriptSource.class);
    assertNotNull(JsExecutor.create(ss, b -> {}));

    Context ctx = mock(Context.class);
    assertNotNull(JsExecutor.createWithContext(ctx, ss));
  }
}
