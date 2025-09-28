package io.github.ih0rd.adapter.api.executors;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.*;

class PyExecutorTest {

  private static Path tempDir;

  public interface MyApi {
    int add(int a, int b);

    void ping();
  }

  @BeforeAll
  static void setup() throws IOException {
    tempDir = Files.createTempDirectory("pyexec-test");
    Files.writeString(
        tempDir.resolve("my_api.py"),
        "class MyApi:\n"
            + "    def add(self, a, b):\n"
            + "        return a + b\n\n"
            + "    def ping(self):\n"
            + "        return None\n\n"
            + "import polyglot\n"
            + "polyglot.export_value('MyApi', MyApi)\n");
  }

  @AfterAll
  static void cleanup() throws IOException {
    if (tempDir != null) {
      Files.walk(tempDir)
          .sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
              });
    }
  }

  @Test
  void testEvaluateAddFromFileSystem() {
    var builder = new PolyglotContextFactory.Builder(Language.PYTHON).resourcesPath(tempDir);

    try (PyExecutor exec = PyExecutor.create(builder)) {
      Map<String, Object> result = exec.evaluate("add", MyApi.class, 2, 3);
      assertEquals("int", result.get("returnType"));
      assertEquals(5, result.get("result"));
    }
  }

  @Test
  void testEvaluatePingReturnsEmpty() {
    var builder = new PolyglotContextFactory.Builder(Language.PYTHON).resourcesPath(tempDir);

    try (PyExecutor exec = PyExecutor.create(builder)) {
      Map<String, Object> result = exec.evaluate("ping", MyApi.class);
      assertEquals("void", result.get("returnType"));
      assertEquals(Optional.empty(), result.get("result"));
    }
  }

  @Test
  void testInvalidMethodThrows() {
    var builder = new PolyglotContextFactory.Builder(Language.PYTHON).resourcesPath(tempDir);

    try (PyExecutor exec = PyExecutor.create(builder)) {
      assertThrows(EvaluationException.class, () -> exec.evaluate("nonexistent", MyApi.class));
    }
  }
}
