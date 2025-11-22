package io.github.ih0rd.adapter.context;

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
}
