package io.github.ih0rd.adapter.api.context;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ResourcesProviderOverrideTest {

  private String prevPy;
  private String prevJs;

  @AfterEach
  void tearDown() {
    if (prevPy != null) System.setProperty("py.polyglot-resources.path", prevPy);
    else System.clearProperty("py.polyglot-resources.path");
    if (prevJs != null) System.setProperty("js.polyglot-resources.path", prevJs);
    else System.clearProperty("js.polyglot-resources.path");
  }

  @Test
  void get_usesSystemPropertyOverrides() {
    prevPy = System.getProperty("py.polyglot-resources.path");
    prevJs = System.getProperty("js.polyglot-resources.path");

    Path customPy = Paths.get("/tmp/custom_py");
    Path customJs = Paths.get("/tmp/custom_js");

    System.setProperty("py.polyglot-resources.path", customPy.toString());
    System.setProperty("js.polyglot-resources.path", customJs.toString());

    assertEquals(customPy, ResourcesProvider.get(Language.PYTHON));
    assertEquals(customJs, ResourcesProvider.get(Language.JS));
  }
}
