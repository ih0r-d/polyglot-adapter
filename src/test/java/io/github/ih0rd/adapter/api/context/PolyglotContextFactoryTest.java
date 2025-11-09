package io.github.ih0rd.adapter.api.context;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.Map;

import org.graalvm.polyglot.*;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PolyglotContextFactoryTest {

  Context.Builder ctxBuilder;
  Context mockContext;

  @BeforeEach
  void setup() {
    ctxBuilder = mock(Context.Builder.class, RETURNS_SELF);
    mockContext = mock(Context.class);
    when(ctxBuilder.build()).thenReturn(mockContext);
  }

  @Test
  void shouldCreateDefaultPythonContext() {
    try (MockedStatic<GraalPyResources> graal = mockStatic(GraalPyResources.class);
        MockedStatic<VirtualFileSystem> vfs = mockStatic(VirtualFileSystem.class)) {

      var fakeVfs = mock(VirtualFileSystem.class);
      var vfsBuilder = mock(VirtualFileSystem.Builder.class, RETURNS_SELF);
      when(vfsBuilder.build()).thenReturn(fakeVfs);
      vfs.when(VirtualFileSystem::newBuilder).thenReturn(vfsBuilder);
      graal
          .when(() -> GraalPyResources.contextBuilder(any(VirtualFileSystem.class)))
          .thenReturn(ctxBuilder);

      var ctx =
          new PolyglotContextFactory.Builder(SupportedLanguage.PYTHON)
              .withSafePythonDefaults()
              .build();

      assertThat(ctx).isEqualTo(mockContext);
    }
  }

  @Test
  void shouldCreateDefaultJsContextWithNodeSupport() {
    try (MockedStatic<Context> st = mockStatic(Context.class)) {
      st.when(() -> Context.newBuilder(anyString())).thenReturn(ctxBuilder);

      var ctx =
          new PolyglotContextFactory.Builder(SupportedLanguage.JS)
              .withNodeSupport()
              .option("custom.option", "true")
              .build();

      assertThat(ctx).isEqualTo(mockContext);
      st.verify(() -> Context.newBuilder("js"));
    }
  }

  @Test
  void shouldApplyHostAccessAndExtender() {
    HostAccess.Builder haBuilder = mock(HostAccess.Builder.class, RETURNS_SELF);
    try (MockedStatic<HostAccess> st = mockStatic(HostAccess.class)) {
      st.when(() -> HostAccess.newBuilder(any())).thenReturn(haBuilder);
      when(haBuilder.build()).thenReturn(mock(HostAccess.class));

      try (MockedStatic<Context> ctxSt = mockStatic(Context.class)) {
        ctxSt.when(() -> Context.newBuilder(anyString())).thenReturn(ctxBuilder);

        var builder =
            new PolyglotContextFactory.Builder(SupportedLanguage.JS)
                .extendHostAccess(
                    b ->
                        b.targetTypeMapping(
                            Value.class, Path.class, Value::isString, v -> Path.of(v.asString())))
                .hostAccess(HostAccess.ALL)
                .apply(b -> b.option("engine.WarnInterpreterOnly", "false"))
                .build();

        assertThat(builder).isEqualTo(mockContext);
      }
    }
  }

  @Test
  void shouldSetOptionsAndResourceDirectory() {
    var b =
        new PolyglotContextFactory.Builder(SupportedLanguage.JS)
            .option("js.console", "true")
            .options(Map.of("a", "b"))
            .resourceDirectory("custom.vfs");

    assertThat(b).isNotNull();
  }

  @Test
  void shouldThrowOnUnsupportedLanguage() {
    var lang = mock(SupportedLanguage.class);
    when(lang.id()).thenReturn("weird");
    when(lang.name()).thenReturn("UNKNOWN");

    var b = new PolyglotContextFactory.Builder(SupportedLanguage.JS);
    assertThatThrownBy(() -> new PolyglotContextFactory.Builder(lang).build())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldCreateDefaultContext() {
    try (MockedStatic<Context> st = mockStatic(Context.class)) {
      st.when(() -> Context.newBuilder(anyString())).thenReturn(ctxBuilder);
      var ctx = PolyglotContextFactory.createDefault(SupportedLanguage.JS);
      assertThat(ctx).isEqualTo(mockContext);
    }
  }

  @Test
  void shouldReturnResourcesPath() {
    Path fake = Path.of("/tmp");
    var b = new PolyglotContextFactory.Builder(SupportedLanguage.JS).resourcesPath(fake);
    assertThat(b.getResourcesPath()).isEqualTo(fake);
  }
}
