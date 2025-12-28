package io.github.ih0rd.adapter.context;

import static io.github.ih0rd.adapter.utils.Constants.DEFAULT_JS_RESOURCES;
import static io.github.ih0rd.adapter.utils.Constants.DEFAULT_PY_RESOURCES;
import static io.github.ih0rd.adapter.utils.Constants.PROP_JS_RESOURCES;
import static io.github.ih0rd.adapter.utils.Constants.PROP_PY_RESOURCES;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.ih0rd.adapter.script.ScriptSource;

/// @deprecated This class is deprecated in favor of {@link ScriptSource}.
/// All script resolution must be performed via the ScriptSource SPI.
/// This class will be removed in a future release.

@Deprecated(forRemoval = true, since = "0.1.1")
public final class ResourcesProvider {

  private ResourcesProvider() {}

  public static Path get(SupportedLanguage lang) {
    return switch (lang) {
      case PYTHON -> Paths.get(System.getProperty(PROP_PY_RESOURCES, DEFAULT_PY_RESOURCES));
      case JS -> Paths.get(System.getProperty(PROP_JS_RESOURCES, DEFAULT_JS_RESOURCES));
    };
  }
}
