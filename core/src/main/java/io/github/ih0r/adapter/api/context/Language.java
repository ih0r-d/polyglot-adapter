package io.github.ih0r.adapter.api.context;

public enum Language {
  PYTHON("python");

  private final String id;

  Language(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
