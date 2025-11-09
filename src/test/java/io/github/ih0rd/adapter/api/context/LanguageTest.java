package io.github.ih0rd.adapter.api.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LanguageTest {
  @Test
  void id_returnsPython() {
    assertEquals("python", SupportedLanguage.PYTHON.id());
  }

  @Test
  void id_returnsJs() {
    assertEquals("js", SupportedLanguage.JS.id());
  }
}
