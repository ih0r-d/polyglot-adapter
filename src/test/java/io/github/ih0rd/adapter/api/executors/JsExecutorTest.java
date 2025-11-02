package io.github.ih0rd.adapter.api.executors;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for {@link JsExecutor}. */
class JsExecutorTest {

  private static JsExecutor executor;

  @BeforeAll
  static void setup() {
    var builder = new PolyglotContextFactory.Builder(Language.JS);
    executor = JsExecutor.create(builder);
  }

  @AfterAll
  static void cleanup() {
    executor.close();
  }

  @Test
  void evaluate_inlineJs_returnsExpectedResult() {
    EvalResult<?> result = executor.evaluate("1 + 2");
    assertNotNull(result);
    assertEquals(3, result.as(Double.class));
  }

  @Test
  void evaluate_inlineJs_handlesObjects() {
    String js = "({ name: 'John', age: 30 })";
    var result = executor.evaluate(js);
    assertNotNull(result.value());
    var map = (java.util.Map<?, ?>) result.value();
    assertEquals("John", map.get("name"));
    assertEquals(30, ((Number) map.get("age")).intValue());
  }

  @Test
  void evaluate_inlineJs_handlesArrays() {
    var result = executor.evaluate("[1, 2, 3]");
    var list = (java.util.List<?>) result.value();
    assertEquals(3, list.size());
    assertEquals(2.0, list.get(1));
  }

  @Test
  void evaluate_inlineJs_errorThrowsException() {
    assertThrows(RuntimeException.class, () -> executor.evaluate("nonexistentVar + 1"));
  }

  @Test
  void async_evaluateExecutesInVirtualThread() throws ExecutionException, InterruptedException {
    var future = executor.async(() -> 42);
    assertEquals(42, future.get());
  }

  @Test
  void close_doesNotThrow() {
    assertDoesNotThrow(
        () -> {
          try (var localExec = JsExecutor.createDefault()) {
            assertNotNull(localExec);
          }
        });
  }

  @Test
  void bind_createsProxyForInterface() {
    interface MathApi {
      Object add(int a, int b);
    }

    MathApi api = executor.bind(MathApi.class);
    assertNotNull(api);
  }
}
