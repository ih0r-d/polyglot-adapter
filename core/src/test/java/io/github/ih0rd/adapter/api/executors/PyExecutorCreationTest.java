package io.github.ih0rd.adapter.api.executors;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class PyExecutorCreationTest {

  interface MyApi {
    void m();
  }

  private boolean isPythonAvailable() {
    try {
      return Engine.create().getLanguages().containsKey("python");
    } catch (Throwable t) {
      return false;
    }
  }

  @Test
  void createDefault_andClose() {
    Assumptions.assumeTrue(isPythonAvailable(), "Python engine not available; skipping test");
    try (PyExecutor exec = PyExecutor.createDefault()) {
      assertNotNull(exec);
    }
  }

  @Test
  void createWithBuilder_andEvaluateMissingFile_throws() {
    Assumptions.assumeTrue(isPythonAvailable(), "Python engine not available; skipping test");
    PolyglotContextFactory.Builder b = new PolyglotContextFactory.Builder(Language.PYTHON);
    try (PyExecutor exec = PyExecutor.create(b)) {
      EvaluationException ex =
          assertThrows(EvaluationException.class, () -> exec.evaluate("m", MyApi.class));
      assertTrue(ex.getMessage().contains("Cannot find Python file"));
    }
  }
}
