// package io.github.ih0rd.adapter.api.executors;
//
// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;
//
// import java.nio.file.Path;
//
// import org.graalvm.polyglot.*;
// import org.junit.jupiter.api.*;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.*;
// import org.mockito.junit.jupiter.*;
// import org.mockito.quality.Strictness;
//
// import io.github.ih0rd.adapter.DummyApi;
// import io.github.ih0rd.adapter.api.context.PolyglotHelper;
// import io.github.ih0rd.adapter.exceptions.EvaluationException;
//
// @ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.LENIENT)
// class PyExecutorTest {
//
//  @Mock Context context;
//  @Mock Value bindings;
//  @Mock Value pyMember;
//  @Mock Value pyInstance;
//  @Mock Value evalResult;
//  @Mock DummyApi dummyApi;
//
//  Path fakePath = Path.of("src", "test", "python");
//  PyExecutor executor;
//
//  @BeforeEach
//  void setup() {
//    System.setProperty("py.polyglot-resources.path", fakePath.toAbsolutePath().toString());
//    executor = new PyExecutor(context, fakePath);
//  }
//
//  @Test
//  void shouldReturnLanguageId() {
//    assertThat(executor.languageId()).isEqualTo("python");
//  }
//
//  //  @Test
//  //  void shouldThrowWhenInterfaceDoesNotMatchPythonClass() {
//  //    when(context.getPolyglotBindings()).thenReturn(bindings);
//  //    when(bindings.getMemberKeys()).thenReturn(Set.of("WrongName"));
//  //    when(bindings.getMember("WrongName")).thenReturn(pyMember);
//  //    when(pyMember.newInstance()).thenReturn(pyInstance);
//  //    when(pyInstance.as(DummyApi.class)).thenReturn(dummyApi);
//  //
//  //    try (MockedStatic<CommonUtils> st = mockStatic(CommonUtils.class)) {
//  //      st.when(() -> CommonUtils.getFirstElement(any())).thenReturn("WrongName");
//  //      assertThatThrownBy(() -> executor.bind(DummyApi.class))
//  //          .isInstanceOf(EvaluationException.class)
//  //          .hasMessageContaining("must match Python class");
//  //    }
//  //  }
//  //
//  //  @Test
//  //  void shouldInvokePythonMethodSuccessfully() {
//  //    when(context.getPolyglotBindings()).thenReturn(bindings);
//  //    when(bindings.getMemberKeys()).thenReturn(Set.of("DummyApi"));
//  //    when(bindings.getMember("DummyApi")).thenReturn(pyMember);
//  //    when(pyMember.newInstance()).thenReturn(pyInstance);
//  //    when(pyInstance.getMember("add")).thenReturn(evalResult);
//  //    when(evalResult.canExecute()).thenReturn(true);
//  //    when(evalResult.execute(any(Object[].class))).thenReturn(Value.asValue(5));
//  //
//  //    try (MockedStatic<CommonUtils> st = mockStatic(CommonUtils.class)) {
//  //      st.when(() -> CommonUtils.getFirstElement(any())).thenReturn("DummyApi");
//  //      DummyApi api = executor.bind(DummyApi.class);
//  //      assertThat(api.add(1, 2)).isEqualTo(5);
//  //    }
//  //  }
//
//  @Test
//  void shouldThrowIfMethodNotFound() throws Exception {
//    when(pyInstance.getMember("missing")).thenReturn(null);
//    var m =
//        PyExecutor.class.getDeclaredMethod(
//            "invokeMethod", Class.class, Value.class, String.class, Object[].class);
//    m.setAccessible(true);
//    assertThatThrownBy(
//            () -> m.invoke(executor, DummyApi.class, pyInstance, "missing", new Object[] {}))
//        .hasRootCauseInstanceOf(EvaluationException.class);
//  }
//
//  @Test
//  void shouldEvaluateInlinePythonCode() {
//    when(context.eval(any(Source.class))).thenReturn(evalResult);
//    when(evalResult.asInt()).thenReturn(42);
//    var result = executor.evaluate("1+2");
//    assertThat(result).isEqualTo(evalResult);
//  }
//
//  @Test
//  void shouldThrowOnEvalError() {
//    when(context.eval(any(Source.class))).thenThrow(new RuntimeException("boom"));
//    assertThatThrownBy(() -> executor.evaluate("1/0"))
//        .isInstanceOf(EvaluationException.class)
//        .hasMessageContaining("Error during Python eval");
//  }
//
//  @Test
//  @SuppressWarnings("resource")
//  void shouldCreateDefaultExecutor() {
//    try (MockedStatic<PolyglotHelper> helper = mockStatic(PolyglotHelper.class)) {
//      Context ctx = mock(Context.class);
//      helper.when(() -> PolyglotHelper.createPythonContext(true)).thenReturn(ctx);
//
//      var exec = PyExecutor.create(true);
//      assertThat(exec).isNotNull();
//      helper.verify(() -> PolyglotHelper.createPythonContext(true));
//    }
//  }
//
//  @Test
//  void shouldCloseAndClearCache() {
//    executor.close();
//    assertThatCode(executor::close).doesNotThrowAnyException();
//  }
// }
