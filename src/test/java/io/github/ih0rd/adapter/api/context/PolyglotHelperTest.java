package io.github.ih0rd.adapter.api.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.nio.file.Path;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class PolyglotHelperTest {

    @Test
    void shouldCreateEmbeddedPythonContext() {
        try (MockedStatic<VirtualFileSystem> vfsMock = mockStatic(VirtualFileSystem.class);
                MockedStatic<GraalPyResources> graalMock = mockStatic(GraalPyResources.class)) {

            var vfsBuilder = mock(VirtualFileSystem.Builder.class, RETURNS_SELF);
            var fakeVfs = mock(VirtualFileSystem.class);
            when(vfsBuilder.build()).thenReturn(fakeVfs);
            vfsMock.when(VirtualFileSystem::newBuilder).thenReturn(vfsBuilder);

            var ctxBuilder = mock(Context.Builder.class, RETURNS_SELF);
            var fakeCtx = mock(Context.class);
            when(ctxBuilder.build()).thenReturn(fakeCtx);
            graalMock.when(() -> GraalPyResources.contextBuilder(any(VirtualFileSystem.class)))
                    .thenReturn(ctxBuilder);

            var ctx = PolyglotHelper.createPythonContext(true);

            assertThat(ctx).isEqualTo(fakeCtx);
            vfsMock.verify(VirtualFileSystem::newBuilder);
            verify(vfsBuilder).resourceDirectory("org.graalvm.python.vfs");
            graalMock.verify(() -> GraalPyResources.contextBuilder(fakeVfs));
        }
    }

    @Test
    void shouldCreateFilesystemPythonContext() {
        try (MockedStatic<VirtualFileSystem> vfsMock = mockStatic(VirtualFileSystem.class);
                MockedStatic<GraalPyResources> graalMock = mockStatic(GraalPyResources.class);
                MockedStatic<ResourcesProvider> providerMock = mockStatic(ResourcesProvider.class)) {

            var vfsBuilder = mock(VirtualFileSystem.Builder.class, RETURNS_SELF);
            var fakeVfs = mock(VirtualFileSystem.class);
            when(vfsBuilder.build()).thenReturn(fakeVfs);
            vfsMock.when(VirtualFileSystem::newBuilder).thenReturn(vfsBuilder);

            providerMock.when(() -> ResourcesProvider.get(SupportedLanguage.PYTHON))
                    .thenReturn(Path.of("/tmp/test_py"));

            var ctxBuilder = mock(Context.Builder.class, RETURNS_SELF);
            var fakeCtx = mock(Context.class);
            when(ctxBuilder.build()).thenReturn(fakeCtx);
            graalMock.when(() -> GraalPyResources.contextBuilder(any(VirtualFileSystem.class)))
                    .thenReturn(ctxBuilder);

            var ctx = PolyglotHelper.createPythonFsContext(true);

            assertThat(ctx).isEqualTo(fakeCtx);
            verify(vfsBuilder).resourceDirectory("/tmp/test_py");
            graalMock.verify(() -> GraalPyResources.contextBuilder(fakeVfs));
        }
    }

    @Test
    void shouldCreateJsContext() {
        try (MockedStatic<Context> ctxMock = mockStatic(Context.class)) {
            var ctxBuilder = mock(Context.Builder.class, RETURNS_SELF);
            var fakeCtx = mock(Context.class);
            when(ctxBuilder.build()).thenReturn(fakeCtx);
            ctxMock.when(() -> Context.newBuilder("js")).thenReturn(ctxBuilder);

            var ctx = PolyglotHelper.createJsContext(true);

            assertThat(ctx).isEqualTo(fakeCtx);
            ctxMock.verify(() -> Context.newBuilder("js"));
            verify(ctxBuilder).option("js.console", "true");
        }
    }

    @Test
    void shouldReturnDefaultHostAccess() {
        HostAccess access = invokeDefaultHostAccess();
        assertThat(access).isNotNull();
        assertThat(access).isInstanceOf(HostAccess.class);
    }

    private HostAccess invokeDefaultHostAccess() {
        try {
            Method m = PolyglotHelper.class.getDeclaredMethod("defaultHostAccess");
            m.setAccessible(true);
            return (HostAccess) m.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
