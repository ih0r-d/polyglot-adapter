package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.DummyApi;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JsExecutorTest {

    @Mock
    Context context;
    @Mock
    Value value;

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
        when(value.asInt()).thenReturn(10);
        when(value.isNull()).thenReturn(false);
        assertThat(executor.evaluate("2+3")).isEqualTo(value);
        verify(context).eval(any(Source.class));
    }

    @Test
    void shouldThrowOnEvalError() {
        when(context.eval(any(Source.class))).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> executor.evaluate("2+2"))
                .isInstanceOf(EvaluationException.class)
                .hasMessageContaining("Error during JS inline execution");
    }

    @Test
    void shouldThrowIfJsFunctionFails() {
        Value bindings = mock(Value.class);
        Value fn = mock(Value.class);
        when(context.eval(any(Source.class))).thenReturn(value);
        when(context.getBindings("js")).thenReturn(bindings);
        when(bindings.getMember("ping")).thenReturn(fn);
        when(fn.canExecute()).thenReturn(true);
        when(fn.execute(any())).thenThrow(new RuntimeException("bad"));

        assertThatThrownBy(() -> executor.evaluate("ping", DummyApi.class))
                .isInstanceOf(EvaluationException.class)
                .hasMessageContaining("Error executing JS function");
    }

    @Test
    void shouldCreateDefaultExecutor() {
        PolyglotContextFactory.Builder builder = mock(PolyglotContextFactory.Builder.class);
        when(builder.build()).thenReturn(context);
        when(builder.getResourcesPath()).thenReturn(fakePath);
        JsExecutor exec = JsExecutor.create(builder);
        assertThat(exec).isNotNull();
    }

    @Test
    void shouldThrowWhenJsFileMissing() {
        JsExecutor jsExec = new JsExecutor(context, Path.of("/non/existent"));
        assertThatThrownBy(() -> jsExec.evaluate("missingFunc", DummyApi.class))
                .isInstanceOf(EvaluationException.class);
    }

    @Test
    void shouldLoadSourceFromFilesystemWhenClasspathMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("js_test");
        Path jsFile = tempDir.resolve("DummyApi.js");
        Files.writeString(jsFile, "function ping(){return 123;}", StandardCharsets.UTF_8);

        JsExecutor ex = new JsExecutor(context, tempDir);

        var m = JsExecutor.class.getDeclaredMethod("loadSource", Class.class);
        m.setAccessible(true);

        Object src = m.invoke(ex, DummyApi.class);

        assertThat(src).isInstanceOf(Source.class);
        assertThat(src.toString()).contains("DummyApi.js");
    }


}
