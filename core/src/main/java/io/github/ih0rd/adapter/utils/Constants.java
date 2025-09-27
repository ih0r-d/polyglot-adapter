package io.github.ih0rd.adapter.utils;

public interface Constants {

  // -----------------------------
  // Language identifiers
  // -----------------------------
  String PYTHON = "python";
  String JS = "js";

  // -----------------------------
  // System property keys
  // -----------------------------
  String PROP_PY_RESOURCES = "polyglot.py-resources.path";
  String PROP_JS_RESOURCES = "polyglot.js-resources.path";

  // -----------------------------
  // Base project dir
  // -----------------------------
  String USER_DIR = System.getProperty("user.dir");

  // -----------------------------
  // Default resource paths
  // -----------------------------
  String DEFAULT_PY_RESOURCES = USER_DIR + "/src/main/python";
  String DEFAULT_JS_RESOURCES = USER_DIR + "/src/main/js";

  // -----------------------------
  // Effective resource paths (resolved from -Dprops or defaults)
  // -----------------------------
  String PROJ_PY_RESOURCES_PATH = System.getProperty(PROP_PY_RESOURCES, DEFAULT_PY_RESOURCES);

  String PROJ_JS_RESOURCES_PATH = System.getProperty(PROP_JS_RESOURCES, DEFAULT_JS_RESOURCES);
}
