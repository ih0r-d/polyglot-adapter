package io.github.ih0rd.adapter.api.context;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PolyglotContextFactoryTest {

    @Test
    void builder_initialValuesAndPath() {
        var builder = new PolyglotContextFactory.Builder(Language.PYTHON);
        assertNotNull(builder.getResourcesPath());
        assertTrue(builder.getResourcesPath().toString().contains("python"));
    }

    @Test
    void setters_doNotThrow() {
        var builder = new PolyglotContextFactory.Builder(Language.JS)
                .allowExperimentalOptions(true)
                .allowAllAccess(false)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .polyglotAccess(org.graalvm.polyglot.PolyglotAccess.NONE)
                .hostAccess(org.graalvm.polyglot.HostAccess.NONE)
                .resourceDirectory("mock-dir");
        assertTrue(builder.getResourcesPath().toString().contains("js"));
    }
}
