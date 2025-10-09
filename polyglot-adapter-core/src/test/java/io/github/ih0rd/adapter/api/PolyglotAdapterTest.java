package io.github.ih0rd.adapter.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.executors.BaseExecutor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PolyglotAdapterTest {

  static class FakeExecutor implements BaseExecutor {
    AtomicBoolean closed = new AtomicBoolean(false);
    String lastMethod;
    Class<?> lastType;
    Object[] lastArgs;

    @Override
    public <T> EvalResult<?> evaluate(
        String methodName, Class<T> memberTargetType, Object... args) {
      this.lastMethod = methodName;
      this.lastType = memberTargetType;
      this.lastArgs = args;
      return  EvalResult.of("ok");
    }

    @Override
    public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType) {
      this.lastMethod = methodName;
      this.lastType = memberTargetType;
      this.lastArgs = new Object[0];
      return EvalResult.of("ok");
    }

    @Override
    public void close() {
      closed.set(true);
    }
  }

  interface MyApi {
    void m();
  }

  @Test
  void delegatesEvaluateWithArgsAndNoArgsAndClose() {
    FakeExecutor exec = new FakeExecutor();
    try (PolyglotAdapter adapter = PolyglotAdapter.of(exec)) {
      var r1 = adapter.evaluate("sum", MyApi.class, 1, 2);
      assertEquals(Boolean.TRUE, r1.value());
      assertEquals("sum", exec.lastMethod);
      assertEquals(MyApi.class, exec.lastType);
      assertArrayEquals(new Object[] {1, 2}, exec.lastArgs);

      var r2 = adapter.evaluate("ping", MyApi.class);
      assertEquals(Boolean.TRUE, r2.value());
      assertEquals("ping", exec.lastMethod);
      assertEquals(MyApi.class, exec.lastType);
      assertArrayEquals(new Object[] {}, exec.lastArgs);
    }
    assertTrue(exec.closed.get());
  }
}
