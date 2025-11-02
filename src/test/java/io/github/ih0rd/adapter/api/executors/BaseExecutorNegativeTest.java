package io.github.ih0rd.adapter.api.executors;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;

import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

@SuppressWarnings("resource")
class BaseExecutorNegativeTest {

  static class NoContextExec extends BaseExecutor {
    NoContextExec() {
      super(null, Paths.get("/tmp"));
    }

    @Override
    public String languageId() {
      return Language.JS.id();
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(
        String m, Class<T> c, Object... a) {
      return null;
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(String m, Class<T> c) {
      return null;
    }
  }

  @Test
  void evaluate_inline_throwsEvaluationException_whenContextNull() {
    var exec = new NoContextExec();
    assertThrows(EvaluationException.class, () -> exec.evaluate("1+1"));
  }

  static class LoaderExec extends BaseExecutor {
    LoaderExec(java.nio.file.Path resources) {
      super(null, resources);
    }

    @Override
    public String languageId() {
      return Language.JS.id();
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(
        String m, Class<T> c, Object... a) {
      return null;
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(String m, Class<T> c) {
      return null;
    }

    SourceAccessor expose() {
      return new SourceAccessor(this);
    }
  }

  record SourceAccessor(LoaderExec ex) {

    Source callLoad(String name) {
      return ex.loadScript(Language.JS, name);
    }
  }

  @Test
  void loadScript_throwsWhenMissingOnClasspathAndFs() {
    var exec = new LoaderExec(Paths.get("/path/that/does/not/exist"));
    var accessor = exec.expose();
    var ex = assertThrows(EvaluationException.class, () -> accessor.callLoad("DefinitelyMissing"));
    assertTrue(ex.getMessage().contains("Cannot find script"));
  }
}
