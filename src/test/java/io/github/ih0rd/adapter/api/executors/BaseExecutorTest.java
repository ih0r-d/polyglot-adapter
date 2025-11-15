package io.github.ih0rd.adapter.api.executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.nio.file.Path;

import org.graalvm.polyglot.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;

import io.github.ih0rd.adapter.api.context.SupportedLanguage;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaseExecutorTest {

  @Mock Context context;
  @Mock Value value;
  Path fakePath = Path.of("/tmp");
  BaseExecutor executor;

  @BeforeEach
  void setup() {
    executor =
        new BaseExecutor(context, fakePath) {
          @Override
          public String languageId() {
            return "python";
          }
        };
  }

  @Test
  void shouldEvaluateInlineCode() {
    when(context.eval(any(Source.class))).thenReturn(value);
    assertThat(executor.evaluate("1+1")).isEqualTo(value);
  }

  @Test
  void shouldThrowWhenFunctionNotFound() {
    Value bindings = mock(Value.class);
    when(context.getBindings("python")).thenReturn(bindings);
    when(bindings.getMember("add")).thenReturn(null);

    assertThatThrownBy(() -> executor.callFunction("add", 1, 2))
        .isInstanceOf(EvaluationException.class)
        .hasMessageContaining("Error executing function: add")
        .hasCauseInstanceOf(EvaluationException.class)
        .cause()
        .hasMessageContaining("Function not found");
  }

  @Test
  void shouldExecuteCallFunction() {
    Value bindings = mock(Value.class);
    Value fn = mock(Value.class);
    when(context.getBindings("python")).thenReturn(bindings);
    when(bindings.getMember("add")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);
    when(fn.execute(any(Object[].class))).thenReturn(value);
    assertThat(executor.callFunction("add", 1, 2)).isEqualTo(value);
  }

  @Test
  void shouldThrowWhenEvalFails() {
    when(context.eval(any(Source.class))).thenThrow(new RuntimeException("boom"));
    assertThatThrownBy(() -> executor.evaluate("x+y"))
        .isInstanceOf(EvaluationException.class)
        .hasMessageContaining("Error during python evaluation");
  }

  @Test
  void shouldCloseContext() {
    executor.close();
    verify(context).close();
  }

  @Test
  void shouldFailOnMissingFile() {
    Path p = Path.of("/non/existent");
    BaseExecutor ex =
        new BaseExecutor(context, p) {
          @Override
          public String languageId() {
            return "python";
          }
        };
    assertThatThrownBy(() -> ex.loadScript(SupportedLanguage.PYTHON, "fake"))
        .isInstanceOf(EvaluationException.class);
  }

  @Test
  void shouldHandleFunctionExecutionError() {
    Value bindings = mock(Value.class);
    Value fn = mock(Value.class);
    when(context.getBindings("python")).thenReturn(bindings);
    when(bindings.getMember("sum")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);
    doThrow(new RuntimeException("bad")).when(fn).execute(any(Object[].class));

    assertThatThrownBy(() -> executor.callFunction("sum", 1, 2))
        .isInstanceOf(EvaluationException.class)
        .hasMessageContaining("Error executing function: sum")
        .cause()
        .hasMessage("bad");
  }

  @Test
  void shouldLoadScriptFromClasspath() {
    ClassLoader cl = mock(ClassLoader.class);
    InputStream is = new java.io.ByteArrayInputStream("print(1)".getBytes());
    when(cl.getResourceAsStream(anyString())).thenReturn(is);
    Thread.currentThread().setContextClassLoader(cl);

    var src = executor.loadScript(SupportedLanguage.PYTHON, "dummy_api");
    assertThat(src).isNotNull();
  }

  //  @Test
  //  void shouldCacheSource() {
  //    var src1 = mock(Source.class);
  //    var src2 = mock(Source.class);
  //    executor.sourceCache.put(anyString(), src1);
  //    executor.sourceCache.computeIfAbsent(anyString(), _ -> src2);
  //    assertThat(executor.sourceCache.get(anyString())).isEqualTo(src1);
  //  }

  @Test
  void shouldCallEvaluateWithMockedContext() {
    when(context.eval(any(Source.class))).thenReturn(value);
    var result = executor.evaluate("print('ok')");
    assertThat(result).isEqualTo(value);
  }

  @Test
  void shouldReturnContext() {
    assertThat(executor.context()).isEqualTo(context);
  }
}
