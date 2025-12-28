package io.github.ih0rd.adapter.context;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.*;

import io.github.ih0rd.adapter.utils.Constants;

@Deprecated(forRemoval = true, since = "0.1.1")
class ResourcesProviderTest {

  private static final String PY_PROP = "py.polyglot-resources.path";
  private static final String JS_PROP = "js.polyglot-resources.path";

  @AfterEach
  void cleanup() {
    System.clearProperty(PY_PROP);
    System.clearProperty(JS_PROP);
  }

  @Test
  void pythonDefaultPath() {
    System.clearProperty(PY_PROP);

    Path result = ResourcesProvider.get(SupportedLanguage.PYTHON);

    assertEquals(Path.of(Constants.DEFAULT_PY_RESOURCES), result);
  }

  @Test
  void pythonCustomPath() {
    System.setProperty(PY_PROP, "/opt/custom/py");

    Path result = ResourcesProvider.get(SupportedLanguage.PYTHON);

    assertEquals(Path.of("/opt/custom/py"), result);
  }

  @Test
  void jsDefaultPath() {
    System.clearProperty(JS_PROP);

    Path result = ResourcesProvider.get(SupportedLanguage.JS);

    assertEquals(Path.of(Constants.DEFAULT_JS_RESOURCES), result);
  }

  @Test
  void jsCustomPath() {
    System.setProperty(JS_PROP, "/opt/custom/js");

    Path result = ResourcesProvider.get(SupportedLanguage.JS);

    assertEquals(Path.of("/opt/custom/js"), result);
  }
}
