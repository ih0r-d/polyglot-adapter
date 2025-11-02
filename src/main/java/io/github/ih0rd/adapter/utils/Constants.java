package io.github.ih0rd.adapter.utils;

/// # Constants
/// Global constant definitions for the Polyglot Adapter framework.
///
/// ---
/// ### Purpose
/// Provides centralized configuration keys, default paths, and resolved system property values
/// for all supported GraalVM languages (e.g., Python and JavaScript).
///
/// ---
/// ### System properties
/// - `py.polyglot-resources.path` — override base path for Python resources.
/// - `js.polyglot-resources.path` — override base path for JavaScript resources.
///
/// These can be set via JVM flags:
/// ```bash
/// -Dpy.polyglot-resources.path=/opt/app/scripts/python
/// -Djs.polyglot-resources.path=/opt/app/scripts/js
/// ```
///
/// ---
/// ### Default directories
/// - Python → `${user.dir}/src/main/python`
/// - JavaScript → `${user.dir}/src/main/js`
///
/// ---
/// ### Example
/// ```java
/// Path pyDir = Paths.get(Constants.PROJ_PY_RESOURCES_PATH);
/// Path jsDir = Paths.get(Constants.PROJ_JS_RESOURCES_PATH);
/// ```
public interface Constants {

  /// ### System property keys
  /// JVM-level property names used for language resource configuration.
  String PROP_PY_RESOURCES = "py.polyglot-resources.path";
  String PROP_JS_RESOURCES = "js.polyglot-resources.path";

  /// ### Base project directory
  /// The root working directory (`user.dir`) from which relative paths are resolved.
  String USER_DIR = System.getProperty("user.dir");

  /// ### Default resource paths
  /// Fallback directories used when no `-D` property is provided.
  String DEFAULT_PY_RESOURCES = USER_DIR + "/src/main/python";
  String DEFAULT_JS_RESOURCES = USER_DIR + "/src/main/js";

  /// ### Effective resource paths
  /// Dynamically resolved directories based on system properties or defaults.
  String PROJ_PY_RESOURCES_PATH = System.getProperty(PROP_PY_RESOURCES, DEFAULT_PY_RESOURCES);
  String PROJ_JS_RESOURCES_PATH = System.getProperty(PROP_JS_RESOURCES, DEFAULT_JS_RESOURCES);
}
