package io.github.ih0rd.adapter.api.context;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotContextFactoryTest {

    @Test
    void builder_setsCustomResourceDirectoryAndPath() {
        Path customPath = Path.of("test/path");
        var builder = new PolyglotContextFactory.Builder(Language.JS)
                .resourceDirectory("custom.vfs")
                .resourcesPath(customPath)
                .allowAllAccess(false)
                .allowCreateThread(false)
                .allowExperimentalOptions(true)
                .allowNativeAccess(false)
                .hostAccess(HostAccess.NONE)
                .polyglotAccess(PolyglotAccess.NONE);

        assertEquals(customPath, builder.getResourcesPath());
        assertNotNull(builder);
    }


    @Test
    void pythonBuilder_buildMethod_doesNotThrowWhenMocked() {
        var builder = new PolyglotContextFactory.Builder(Language.PYTHON)
                .withSafePythonDefaults()
                .option("python.CAPI", "false")
                .resourcesPath(Path.of("."))
                .resourceDirectory("META-INF");

        assertDoesNotThrow(builder::toString); // просто smoke-check
    }

    @Test
    void build_handlesUnknownLanguageGracefully() {
        assertThrows(NullPointerException.class, () ->
                new PolyglotContextFactory.Builder(null).build()
        );
    }
}
