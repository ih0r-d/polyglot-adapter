package io.github.ih0rd.adapter.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.StringReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.exceptions.EvaluationException;
import io.github.ih0rd.adapter.exceptions.ScriptNotFoundException;
import io.github.ih0rd.adapter.script.ScriptSource;

class LoadScriptTest {

  static class TestExecutor extends AbstractPolyglotExecutor {
    TestExecutor(Context ctx, ScriptSource ss) {
      super(ctx, ss);
    }

    @Override
    public String languageId() {
      return "js";
    }

    @Override
    protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
      return null;
    }

    @Override
    protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
      return null;
    }

    // Expose protected method
    public Source callLoadScript(SupportedLanguage lang, String name) {
      return super.loadScript(lang, name);
    }
  }

  @Test
  void loadScript_success() throws Exception {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);

    when(ss.exists(eq(SupportedLanguage.JS), eq("test"))).thenReturn(true);
    when(ss.open(eq(SupportedLanguage.JS), eq("test"))).thenReturn(new StringReader("print('hi')"));

    TestExecutor exec = new TestExecutor(ctx, ss);
    Source src = exec.callLoadScript(SupportedLanguage.JS, "test");

    assertNotNull(src);
    assertEquals("js", src.getLanguage());
    assertTrue(src.getCharacters().toString().contains("print"));
  }

  @Test
  void loadScript_notFound_throws() {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);

    when(ss.exists(any(), any())).thenReturn(false);

    TestExecutor exec = new TestExecutor(ctx, ss);

    assertThrows(
        ScriptNotFoundException.class, () -> exec.callLoadScript(SupportedLanguage.JS, "missing"));
  }

  @Test
  void loadScript_ioException_throws() throws Exception {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);

    when(ss.exists(any(), any())).thenReturn(true);
    when(ss.open(any(), any())).thenThrow(new IOException("disk error"));

    TestExecutor exec = new TestExecutor(ctx, ss);

    assertThrows(
        EvaluationException.class, () -> exec.callLoadScript(SupportedLanguage.JS, "broken"));
  }

  @Test
  void validateBinding_default_throws() {
    Context ctx = mock(Context.class);
    ScriptSource ss = mock(ScriptSource.class);
    TestExecutor exec = new TestExecutor(ctx, ss);

    assertThrows(UnsupportedOperationException.class, () -> exec.validateBinding(String.class));
    assertThrows(IllegalArgumentException.class, () -> exec.validateBinding(null));
  }
}
