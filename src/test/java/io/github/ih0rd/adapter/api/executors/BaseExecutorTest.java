package io.github.ih0rd.adapter.api.executors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class BaseExecutorTest {

  @Test
  void async_executesInVirtualThread() throws Exception {
    var exec = new DummyExecutor();
    var future = exec.async(() -> 123);
    assertEquals(123, future.get());
  }

  @Test
  void bind_invokesThroughProxy() {
    var exec = new DummyExecutor();
    DummyApi api = exec.bind(DummyApi.class);
    assertEquals("ok", api.ping());
  }

  interface DummyApi {
    String ping();
  }

  static class DummyExecutor extends BaseExecutor {
    DummyExecutor() {
      super(null, null);
    }

    public String languageId() {
      return "js";
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(
        String m, Class<T> c, Object... a) {
      return io.github.ih0rd.adapter.api.context.EvalResult.of("ok");
    }

    @Override
    protected <T> io.github.ih0rd.adapter.api.context.EvalResult<?> evaluate(String m, Class<T> c) {
      return io.github.ih0rd.adapter.api.context.EvalResult.of("ok");
    }
  }
}
