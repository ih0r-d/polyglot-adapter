package io.github.ih0rd.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SupportedLanguageTest {

  @Test
  void pythonId() {
    assertEquals("python", SupportedLanguage.PYTHON.id());
  }

  @Test
  void pythonExt() {
    assertEquals(".py", SupportedLanguage.PYTHON.ext());
  }

  @Test
  void jsId() {
    assertEquals("js", SupportedLanguage.JS.id());
  }

  @Test
  void jsExt() {
    assertEquals(".js", SupportedLanguage.JS.ext());
  }
}
