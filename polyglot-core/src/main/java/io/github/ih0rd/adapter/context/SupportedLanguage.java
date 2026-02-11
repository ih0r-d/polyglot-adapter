package io.github.ih0rd.adapter.context;

import java.util.Arrays;

public enum SupportedLanguage {
  PYTHON("python", ".py"),
  JS("js", ".js");

  private final String id;
  private final String ext;

  SupportedLanguage(String id, String ext) {
    this.id = id;
    this.ext = ext;
  }

  public String id() {
    return id;
  }

  public String ext() {
    return ext;
  }

  public static SupportedLanguage fromFileName(String fileName) {
    return Arrays.stream(values())
        .filter(l -> fileName.endsWith(l.ext()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported file: " + fileName));
  }
}
