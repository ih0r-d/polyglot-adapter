package io.github.ih0rd.adapter.api.context;

import static io.github.ih0rd.adapter.utils.Constants.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourcesProvider {

  private ResourcesProvider() {}

  public static Path get(SupportedLanguage lang) {
    return switch (lang) {
      case PYTHON -> Paths.get(System.getProperty(PROP_PY_RESOURCES, DEFAULT_PY_RESOURCES));
      case JS -> Paths.get(System.getProperty(PROP_JS_RESOURCES, DEFAULT_JS_RESOURCES));
    };
  }
}
