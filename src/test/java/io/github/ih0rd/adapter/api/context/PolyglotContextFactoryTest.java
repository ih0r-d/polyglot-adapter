package io.github.ih0rd.adapter.api.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PolyglotContextFactoryTest {

  @Test
  void createDefault_withoutThrowException() {
    try (var ctx = PolyglotContextFactory.createDefault(Language.PYTHON)) {
      assertNotNull(ctx);
    }
  }

  @Test
  void builderSetters_configuresBuilder() {
    var builder =
        new PolyglotContextFactory.Builder(Language.PYTHON)
            .allowExperimentalOptions(true)
            .allowAllAccess(true)
            .hostAccess(org.graalvm.polyglot.HostAccess.ALL)
            .ioAccess(org.graalvm.polyglot.io.IOAccess.ALL)
            .allowCreateThread(true)
            .allowNativeAccess(true)
            .polyglotAccess(org.graalvm.polyglot.PolyglotAccess.ALL);

    assertNotNull(builder);
  }
}
