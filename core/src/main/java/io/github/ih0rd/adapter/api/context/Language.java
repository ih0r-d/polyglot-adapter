package io.github.ih0rd.adapter.api.context;

public enum Language {
  PYTHON("python"),
  JS("js");

  private final String id;

  Language(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
