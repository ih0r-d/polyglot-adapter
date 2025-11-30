package io.github.ih0rd.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import org.graalvm.polyglot.*;
import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.exceptions.EvaluationException;

@SuppressWarnings({"unchecked", "resource"})
class AbstractPolyglotExecutorTest {

  static class TestExecutor extends AbstractPolyglotExecutor {

    TestExecutor(Context ctx, Path path) {
      super(ctx, path);
    }

    @Override
    public String languageId() {
      return "python";
    }

    @Override
    protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
      Value v = mock(Value.class);
      when(v.as(any(Class.class))).thenReturn("ok");
      return v;
    }

    @Override
    protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
      Value v = mock(Value.class);
      when(v.as(any(Class.class))).thenReturn("noargs");
      return v;
    }
  }

  @Test
  void bindInvokesEvaluate() {
    Context ctx = mock(Context.class);
    TestExecutor exec = spy(new TestExecutor(ctx, Path.of("/tmp")));

    interface Api {
      void hello();
    }

    doReturn(mock(Value.class))
        .when(exec)
        .evaluate(eq("hello"), eq(Api.class), any(Object[].class));

    Api api = exec.bind(Api.class);
    api.hello();

    verify(exec).evaluate(eq("hello"), eq(Api.class), any(Object[].class));
  }

  @Test
  void bindHandlesNullReturn() {
    Context ctx = mock(Context.class);
    TestExecutor exec = spy(new TestExecutor(ctx, Path.of("/tmp")));

    interface Api {
      Object hello();
    }

    doReturn(null).when(exec).evaluate(eq("hello"), eq(Api.class), any(Object[].class));

    Api api = exec.bind(Api.class);
    assertNull(api.hello());
  }

  @Test
  void bindHandlesNullValueObject() {
    Context ctx = mock(Context.class);
    TestExecutor exec = spy(new TestExecutor(ctx, Path.of("/tmp")));

    interface Api {
      Object hello();
    }

    Value nullValue = mock(Value.class);
    when(nullValue.isNull()).thenReturn(true);

    doReturn(nullValue).when(exec).evaluate(eq("hello"), eq(Api.class), any(Object[].class));

    Api api = exec.bind(Api.class);
    assertNull(api.hello());
  }

  @Test
  void evaluateInlineSuccess() {
    Context ctx = mock(Context.class);
    TestExecutor exec = new TestExecutor(ctx, Path.of("/tmp"));

    Value v = mock(Value.class);
    when(ctx.eval(any(Source.class))).thenReturn(v);

    assertSame(v, exec.evaluate("1+1"));
  }

  @Test
  void evaluateInlineThrowsWrappedException() {
    Context ctx = mock(Context.class);
    TestExecutor exec = new TestExecutor(ctx, Path.of("/tmp"));

    when(ctx.eval(any(Source.class))).thenThrow(new RuntimeException("boom"));

    assertThrows(EvaluationException.class, () -> exec.evaluate("x=1"));
  }

  @Test
  void callFunctionExecutes() {
    Context ctx = mock(Context.class);
    Value bindings = mock(Value.class);
    Value fn = mock(Value.class);

    when(ctx.getBindings("python")).thenReturn(bindings);
    when(bindings.getMember("foo")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);

    AbstractPolyglotExecutor exec = new TestExecutor(ctx, Path.of("/tmp"));
    exec.callFunction("foo", 1, 2);

    verify(fn).execute(1, 2);
  }

  @Test
  void callFunctionMissingThrows() {
    Context ctx = mock(Context.class);
    Value bindings = mock(Value.class);

    when(ctx.getBindings("python")).thenReturn(bindings);
    when(bindings.getMember("foo")).thenReturn(null);

    AbstractPolyglotExecutor exec = new TestExecutor(ctx, Path.of("/tmp"));

    assertThrows(EvaluationException.class, () -> exec.callFunction("foo"));
  }

  @Test
  void createDefaultCreatesContextAndPath() {
    Context ctx = mock(Context.class);
    Path p = Path.of("/tmp");

    try (var mocked = mockStatic(PolyglotHelper.class)) {
      mocked.when(() -> PolyglotHelper.newContext(any())).thenReturn(ctx);

      var spyProvider = mockStatic(ResourcesProvider.class);
      spyProvider.when(() -> ResourcesProvider.get(any())).thenReturn(p);

      AbstractPolyglotExecutor ex =
          AbstractPolyglotExecutor.createDefault(SupportedLanguage.PYTHON, TestExecutor::new);

      assertNotNull(ex);
      assertSame(ctx, ex.context);
      spyProvider.close();
    }
  }

  @Test
  void loadScriptFromClasspath() {
    Context ctx = mock(Context.class);
    AbstractPolyglotExecutor exec = new TestExecutor(ctx, Path.of("/tmp"));

    String cp = "python/test.py";
    InputStream is = new ByteArrayInputStream("print('ok')".getBytes(StandardCharsets.UTF_8));

    ClassLoader cl = mock(ClassLoader.class);
    Thread.currentThread().setContextClassLoader(cl);
    when(cl.getResourceAsStream(cp)).thenReturn(is);

    Source src = exec.loadScript(SupportedLanguage.PYTHON, "test");
    assertNotNull(src);
  }

  @Test
  void loadScriptFromFilesystem() throws Exception {
    Path temp = Files.createTempDirectory("py");
    Path script = temp.resolve("test.py");
    Files.writeString(script, "print('fs')");

    AbstractPolyglotExecutor exec = new TestExecutor(mock(Context.class), temp);

    assertNotNull(exec.loadScript(SupportedLanguage.PYTHON, "test"));
  }

  @Test
  void loadScriptNotFoundThrows() {
    Context ctx = mock(Context.class);
    Path p = Path.of("/tmp/does_not_exist_xyz");

    AbstractPolyglotExecutor exec = new TestExecutor(ctx, p);

    assertThrows(
        EvaluationException.class, () -> exec.loadScript(SupportedLanguage.PYTHON, "missing"));
  }

  @Test
  void closeClosesContext() {
    Context ctx = mock(Context.class);
    AbstractPolyglotExecutor exec = new TestExecutor(ctx, Path.of("/tmp"));
    exec.close();
    verify(ctx).close();
  }
}
