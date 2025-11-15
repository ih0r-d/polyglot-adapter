package io.github.ih0rd.adapter.api.executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import org.graalvm.polyglot.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;

import io.github.ih0rd.adapter.DummyApi;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JsExecutorTest {

  @Mock Context context;
  @Mock Value value;

  Path fakePath = Path.of("src", "test", "js");
  JsExecutor executor;

  @BeforeEach
  void setup() {
    System.setProperty("js.polyglot-resources.path", fakePath.toAbsolutePath().toString());
    executor = new JsExecutor(context, fakePath);
  }

  @Test
  void shouldReturnLanguageId() {
    assertThat(executor.languageId()).isEqualTo("js");
  }

  @Test
  void shouldEvaluateInlineCode() {
    when(context.eval(any(Source.class))).thenReturn(value);
    assertThat(executor.evaluate("2+3")).isEqualTo(value);
    verify(context).eval(any(Source.class));
  }

  @Test
  void shouldThrowOnEvalError() {
    when(context.eval(any(Source.class))).thenThrow(new RuntimeException("boom"));
    assertThatThrownBy(() -> executor.evaluate("2+2"))
        .isInstanceOf(EvaluationException.class)
        .hasMessageContaining("Error during JS eval");
  }

  @Test
  void shouldThrowIfJsFunctionFails() {
    Value bindings = mock(Value.class);
    Value fn = mock(Value.class);
    when(context.getBindings("js")).thenReturn(bindings);
    when(bindings.getMember("ping")).thenReturn(fn);
    when(fn.canExecute()).thenReturn(true);
    when(fn.execute(any(Object[].class))).thenThrow(new RuntimeException("bad"));

    assertThatThrownBy(() -> executor.callFunction("ping", 1))
        .isInstanceOf(EvaluationException.class)
        .hasMessageContaining("Error executing function: ping");
  }

  @Test
  void shouldCreateDefaultExecutor() {
    JsExecutor exec = JsExecutor.createDefault();
    assertThat(exec).isNotNull();
    exec.close();
  }

  @Test
  void shouldCreateWithNodeSupport() {
    JsExecutor exec = JsExecutor.createWithNode();
    assertThat(exec).isNotNull();
    exec.close();
  }

  @Test
  void shouldThrowWhenJsFileMissing() {
    JsExecutor jsExec = new JsExecutor(context, Path.of("/non/existent"));
    assertThatThrownBy(() -> jsExec.bind(DummyApi.class)).isInstanceOf(EvaluationException.class);
  }

  @Test
  void shouldLoadSourceFromFilesystemWhenClasspathMissing() throws Exception {
    Path tempDir = Files.createTempDirectory("js_test");
    Path jsFile = tempDir.resolve("DummyApi.js");
    Files.writeString(jsFile, "function ping(){return 123;}", StandardCharsets.UTF_8);

    System.setProperty("js.polyglot-resources.path", tempDir.toAbsolutePath().toString());

    JsExecutor ex = new JsExecutor(context, tempDir);
    when(context.eval(any(Source.class))).thenReturn(Value.asValue(null));

    ArgumentCaptor<Source> captor = ArgumentCaptor.forClass(Source.class);
    ex.bind(DummyApi.class);

    verify(context).eval(captor.capture());
    Source used = captor.getValue();

    assertThat(used).isNotNull();
    assertThat(used.getName()).contains("DummyApi.js");
  }
}
