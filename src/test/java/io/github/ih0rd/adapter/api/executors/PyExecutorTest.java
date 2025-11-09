package io.github.ih0rd.adapter.api.executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.ih0rd.adapter.DummyApi;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import io.github.ih0rd.adapter.utils.CommonUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PyExecutorTest {

  @Mock Context context;
  @Mock Value bindings;
  @Mock Value pyMember;
  @Mock Value pyInstance;
  @Mock Value evalResult;
  @Mock DummyApi dummyApi;

  Path fakePath = Path.of("src", "test", "python");
  PyExecutor executor;

  @BeforeEach
  void setup() {
    System.setProperty("py.polyglot-resources.path", fakePath.toAbsolutePath().toString());

    executor = new PyExecutor(context, fakePath);
    BaseExecutor.SOURCE_CACHE.clear();
    try {
      var inst = PyExecutor.class.getDeclaredField("INSTANCE_CACHE");
      inst.setAccessible(true);
      ((Map<?, ?>) inst.get(null)).clear();
    } catch (Exception ignored) {
    }
  }

  @Test
  void shouldThrowOnInvalidClassName() {
    when(context.getPolyglotBindings()).thenReturn(bindings);
    when(bindings.getMemberKeys()).thenReturn(Set.of("WrongName"));
    when(bindings.getMember("WrongName")).thenReturn(pyMember);
    when(pyMember.newInstance()).thenReturn(pyInstance);
    when(pyInstance.as(DummyApi.class)).thenReturn(dummyApi);

    try (MockedStatic<CommonUtils> st = mockStatic(CommonUtils.class)) {
      st.when(() -> CommonUtils.getFirstElement(any())).thenReturn("WrongName");

      DummyApi api = executor.bind(DummyApi.class);
      assertThatThrownBy(() -> api.add(1, 1))
          .isInstanceOf(EvaluationException.class)
          .hasMessageContaining("must equal Python class name");
    }
  }

  @Test
  void shouldReturnLanguageId() {
    assertThat(executor.languageId()).isEqualTo("python");
  }

  @Test
  void shouldCreateFromBuilder() {
    PolyglotContextFactory.Builder builder = mock(PolyglotContextFactory.Builder.class);
    when(builder.build()).thenReturn(context);
    when(builder.getResourcesPath()).thenReturn(fakePath);
    assertThat(PyExecutor.create(builder)).isNotNull();
  }

  @Test
  void shouldComputeFileSource() throws Exception {
    PyExecutor spyExec = Mockito.spy(executor);
    doReturn(mock(Source.class)).when(spyExec).loadScript(any(), any());
    var method = PyExecutor.class.getDeclaredMethod("getFileSource", Class.class);
    method.setAccessible(true);
    Object result = method.invoke(spyExec, DummyApi.class);
    assertThat(result).isNotNull();
  }
}
