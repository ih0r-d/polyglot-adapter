package io.github.ih0rd.adapter.api.executors;
import io.github.ih0rd.adapter.DummyApi;
import io.github.ih0rd.adapter.api.context.SupportedLanguage;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.io.InputStream;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class BaseExecutorTest {

    @Mock
    Context context;

    @Mock
    Value value;

    Path fakePath = Path.of("/tmp");

    BaseExecutor executor;

    @BeforeEach
    void setup() {
        executor = new BaseExecutor(context, fakePath) {
            @Override
            public String languageId() {
                return "python";
            }

            @Override
            protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
                return value;
            }

            @Override
            protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
                return value;
            }
        };
    }

    @Test
    void shouldEvaluateInlineCode() {
        when(context.eval(any(Source.class))).thenReturn(value);
        when(value.asInt()).thenReturn(42);
        when(value.isNull()).thenReturn(false);
        assertThat(executor.evaluate("1+1")).isEqualTo(value);
        verify(context, times(1)).eval(any(Source.class));
    }

    @Test
    void shouldBindInterfaceAndCallMethod() throws Throwable {
        when(value.isNull()).thenReturn(false);
        when(value.asInt()).thenReturn(10);
        when(value.as(any(Class.class))).thenReturn(10);

        DummyApi api = executor.bind(DummyApi.class);
        int res = api.add(2, 3);
        assertThat(res).isEqualTo(10);
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

        Value result = executor.callFunction("add", 1, 2);
        assertThat(result).isEqualTo(value);
    }


    @Test
    void shouldThrowWhenEvalFails() {
        when(context.eval(any(Source.class))).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> executor.evaluate("x+y"))
                .isInstanceOf(EvaluationException.class)
                .hasMessageContaining("Error during python code execution");
    }

    @Test
    void shouldCloseContext() {
        executor.close();
        verify(context).close();
    }

    @Test
    void shouldCacheMethods() throws NoSuchMethodException {
        Method method = DummyApi.class.getDeclaredMethod("add", int.class, int.class);
        Map<Method, String> cache = BaseExecutor.METHOD_CACHE;
        cache.clear();
        String name = cache.computeIfAbsent(method, Method::getName);
        assertThat(name).isEqualTo("add");
    }

    @Test
    void shouldFailOnMissingFile() {
        Path p = Path.of("/non/existent");
        BaseExecutor ex = new BaseExecutor(context, p) {
            @Override
            public String languageId() {
                return "python";
            }

            @Override
            protected <T> Value evaluate(String methodName, Class<T> memberTargetType, Object... args) {
                return null;
            }

            @Override
            protected <T> Value evaluate(String methodName, Class<T> memberTargetType) {
                return null;
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

        doAnswer(invocation -> { throw new RuntimeException("bad"); })
                .when(fn)
                .execute(any(Object[].class));

        assertThatThrownBy(() -> executor.callFunction("sum", 1, 2))
                .isInstanceOf(EvaluationException.class)
                .hasMessageContaining("Error executing function: sum")
                .hasCauseInstanceOf(RuntimeException.class)
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

    @Test
    void shouldCreateDefaultExecutor() {
        var mockCtx = mock(Context.class);
        var exec = new PyExecutor(mockCtx, Path.of("/tmp"));

        assertThat(exec).isNotNull();
        verifyNoInteractions(mockCtx);
    }

    @Test
    void shouldCreateExecutorWithCustomBuilder() {
        var builder = mock(io.github.ih0rd.adapter.api.context.PolyglotContextFactory.Builder.class);
        when(builder.build()).thenReturn(context);
        when(builder.getResourcesPath()).thenReturn(fakePath);
        var exec = BaseExecutor.create(builder, PyExecutor::new);
        assertThat(exec).isNotNull();
    }



}
