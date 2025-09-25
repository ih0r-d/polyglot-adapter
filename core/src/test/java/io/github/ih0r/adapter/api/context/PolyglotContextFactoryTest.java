package io.github.ih0r.adapter.api.context;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PolyglotContextFactoryTest {

    @Test
    void createDefault_buildsAndCloses() {
        Context ctx = PolyglotContextFactory.createDefault();
        assertNotNull(ctx);
        ctx.close();
    }

    @Test
    void builderSettersAndCreateFromResourceDir_builds() throws Exception {
        Path tmp = Files.createTempDirectory("pyres");
        try {
            PolyglotContextFactory.Builder b = new PolyglotContextFactory.Builder()
                    .allowExperimentalOptions(true)
                    .allowAllAccess(true)
                    .hostAccess(org.graalvm.polyglot.HostAccess.ALL)
                    .ioAccess(org.graalvm.polyglot.io.IOAccess.ALL)
                    .allowCreateThread(true)
                    .allowNativeAccess(true)
                    .polyglotAccess(org.graalvm.polyglot.PolyglotAccess.ALL)
                    .resourceDir(tmp);

            Context ctx = b.build();
            assertNotNull(ctx);
            ctx.close();

            Context ctx2 = PolyglotContextFactory.createFromResourceDir(tmp);
            assertNotNull(ctx2);
            ctx2.close();
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
