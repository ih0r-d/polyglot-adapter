package io.github.ih0rd.adapter.api.executors;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

class BaseExecutorPositiveTest {

  static class JsTestExec extends BaseExecutor {
    JsTestExec(Context ctx) {
      super(ctx, null);
    }

    @Override
    public String languageId() {
      return Language.JS.id();
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(
        String m, Class<T> c, Object... a) {
      return callFunction(m, a);
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(String m, Class<T> c) {
      return callFunction(m);
    }

    SourceAccessor expose() {
      return new SourceAccessor(this);
    }
  }

  record SourceAccessor(JsTestExec ex) {
    Source callLoad() {
      return ex.loadScript(Language.JS, "Foo");
    }

    io.github.ih0rd.adapter.api.context.EvalResult<?> callFn(String name, Object... args) {
      return ex.callFunction(name, args);
    }
  }

  private Context ctx;

  @AfterEach
  void tearDown() {
    if (ctx != null) ctx.close();
  }

  @Test
  void evaluate_inline_returnsValue() {
    ctx = Context.create("js");
    var exec = new JsTestExec(ctx);
    var res = exec.evaluate("40+2");
    assertEquals(42.0, res.value());
  }

  @Test
  void callFunction_throwsWhenMissingFunction() {
    ctx = Context.create("js");
    var exec = new JsTestExec(ctx);
    var accessor = exec.expose();
    assertThrows(EvaluationException.class, () -> accessor.callFn("notExists"));
  }

  @Test
  void loadScript_findsFileOnFilesystem_andFunctionCanExecute() throws IOException {
    Path tempDir = Files.createTempDirectory("js-res");
    try {
      System.setProperty("js.polyglot-resources.path", tempDir.toString());
      Path jsFile = tempDir.resolve("Foo.js");
      Files.writeString(jsFile, "function ping(){return 7}");

      ctx = Context.create("js");
      var exec = new JsTestExec(ctx);
      var accessor = exec.expose();

      Source src = accessor.callLoad();
      assertNotNull(src);

      ctx.eval(src);
      Value bindings = ctx.getBindings("js");
      assertTrue(bindings.getMember("ping").canExecute());
      var result = exec.callFunction("ping");
      assertEquals(7.0, result.value());
    } finally {
      System.clearProperty("js.polyglot-resources.path");
    }
  }
}
