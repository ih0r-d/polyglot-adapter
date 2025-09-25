package io.github.ih0r.adapter.api.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LanguageTest {

  @Test
  void id_returnsPython() {
    assertEquals("python", Language.PYTHON.id());
  }
}
