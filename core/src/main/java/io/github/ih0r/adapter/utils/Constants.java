package io.github.ih0r.adapter.utils;

public interface Constants {

  // Language identifier for Python (used by GraalVM Context)
  String PYTHON = "python";

  // Current working directory of the Java application
  String USER_DIR = System.getProperty("user.dir");

  // Default path for Python resources
  String DEFAULT_PY_RESOURCES = USER_DIR + "/src/main/" + PYTHON;

  /**
   * Project resources path for Python files. Default: ${user.dir}/src/main/python Override with
   * system property: -Dpolyglot.py-resources.path=/custom/path/to/python
   */
  String PROJ_PY_RESOURCES_PATH =
      System.getProperty("polyglot.py-resources.path", DEFAULT_PY_RESOURCES);
}
