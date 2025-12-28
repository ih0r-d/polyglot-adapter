package io.github.ih0rd.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.exceptions.BindingException;
import io.github.ih0rd.adapter.exceptions.InvocationException;
import io.github.ih0rd.adapter.script.ScriptSource;

@SuppressWarnings({"unchecked"})
class AbstractExecutorTest {

  static class TestExecutor extends AbstractPolyglotExecutor {

    TestExecutor(Context ctx) {
      super(ctx, mock(ScriptSource.class));
    }

    @Override
    public String languageId() {
      return "python";
    }

    // critical: avoid Graal init
    @Override
    protected Source loadScript(SupportedLanguage language, String name) {
      return mock(Source.class);
    }

    @Override
    protected <T> Value evaluate(String methodName, Class<T> target, Object... args) {
      Value v = mock(Value.class);
      when(v.isNull()).thenReturn(false);
      when(v.as(any(Class.class))).thenAnswer(inv -> "ok");
      return v;
    }

    @Override
    protected <T> Value evaluate(String methodName, Class<T> target) {
      Value v = mock(Value.class);
      when(v.isNull()).thenReturn(false);
      when(v.as(any(Class.class))).thenAnswer(inv -> "noargs");
      return v;
    }
  }

  @Test
  void bindCallsEvaluate() {
    Context ctx = mock(Context.class);
    TestExecutor exec = spy(new TestExecutor(ctx));

    interface Api {
      String hello();
    }

    Api api = exec.bind(Api.class);
    assertEquals("ok", api.hello());

    verify(exec).evaluate(eq("hello"), eq(Api.class), any(Object[].class));
  }

  @Test
  void bindReturnsNullWhenValueIsNull() {
    Context ctx = mock(Context.class);
    TestExecutor exec = spy(new TestExecutor(ctx));

    Value nullValue = mock(Value.class);
    when(nullValue.isNull()).thenReturn(true);

    doReturn(nullValue).when(exec).evaluate(eq("hello"), any(), any(Object[].class));

    interface Api {
      Object hello();
    }

    Api api = exec.bind(Api.class);
    assertNull(api.hello());
  }

  @Test
  void callFunctionExecutes() {
    Context ctx = mock(Context.class);
    Value bindings = mock(Value.class);
    Value fn = mock(Value.class);

    when(ctx.getBindings("python")).thenReturn(bindings);
    when(bindings.getMember("foo")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);

    TestExecutor exec = new TestExecutor(ctx);
    exec.callFunction("foo", 1, 2);

    verify(fn).execute(1, 2);
  }

  @Test
  void callFunctionMissingThrows() {
    Context ctx = mock(Context.class);
    Value bindings = mock(Value.class);

    when(ctx.getBindings("python")).thenReturn(bindings);
    when(bindings.getMember("foo")).thenReturn(null);

    TestExecutor exec = new TestExecutor(ctx);

    assertThrows(BindingException.class, () -> exec.callFunction("foo"));
  }

  @Test
  void evaluateInlineWrappedException() {
    Context ctx = mock(Context.class);
    TestExecutor exec = new TestExecutor(ctx);

    // real evaluate(String) runs, but Context.eval explodes
    when(ctx.eval(any(Source.class))).thenThrow(new RuntimeException("boom"));

    assertThrows(InvocationException.class, () -> exec.evaluate("x=1"));
  }

  @Test
  void closeClosesContext() {
    Context ctx = mock(Context.class);
    TestExecutor exec = new TestExecutor(ctx);

    exec.close();

    verify(ctx).close();
  }
}
