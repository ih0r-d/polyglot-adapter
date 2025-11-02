package io.github.ih0rd.adapter.api.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResourcesProviderTest {

  @Test
  void get_returnsPythonAndJsPaths() {
    assertTrue(ResourcesProvider.get(Language.PYTHON).toString().endsWith("python"));
    assertTrue(ResourcesProvider.get(Language.JS).toString().endsWith("js"));
  }
}
