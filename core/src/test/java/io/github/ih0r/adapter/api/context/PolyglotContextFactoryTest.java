package io.github.ih0r.adapter.api.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PolyglotContextFactoryTest {

  @Test
  void createDefault_throwsWhenLanguageNotPresent() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          try (var _ = PolyglotContextFactory.createDefault(Language.PYTHON)) {
            fail("Context should not have been created");
          }
        });
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

    assertNotNull(builder); // перевіряємо, що Builder налаштовується
    assertThrows(IllegalStateException.class, builder::build);
  }
}
