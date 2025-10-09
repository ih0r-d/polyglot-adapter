package io.github.ih0rd.adapter.api.context;

import static org.junit.jupiter.api.Assertions.*;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.junit.jupiter.api.Test;

class PolyglotContextFactoryTest {

    @Test
    void createDefault_withoutThrowException() {
        try (var ctx = PolyglotContextFactory.createDefault(Language.PYTHON)) {
            assertNotNull(ctx);
            assertTrue(ctx.getEngine().getLanguages().containsKey("python"));
        }
    }

    @Test
    void builderSetters_configuresBuilder() {
        var builder = new PolyglotContextFactory.Builder(Language.PYTHON)
                .allowExperimentalOptions(true)
                .allowAllAccess(true)
                .hostAccess(HostAccess.ALL)
                .allowCreateThread(true)
                .allowNativeAccess(true)
                .polyglotAccess(PolyglotAccess.ALL)
                .resourceDirectory("org.graalvm.python.vfs");

        assertNotNull(builder);
        assertEquals("org.graalvm.python.vfs", builder.getResourcesPath().getFileName().toString());
    }
}
