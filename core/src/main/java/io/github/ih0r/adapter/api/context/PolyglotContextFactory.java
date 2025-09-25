package io.github.ih0r.adapter.api.context;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.io.IOAccess;

public final class PolyglotContextFactory {

  private PolyglotContextFactory() {}

  public static Context createDefault(Language language) {
    return new Builder(language).build();
  }

  public static final class Builder {
    private final Language language;
    private boolean allowExperimentalOptions = false;
    private boolean allowAllAccess = true;
    private HostAccess hostAccess = HostAccess.ALL;
    private IOAccess ioAccess = IOAccess.ALL;
    private boolean allowCreateThread = true;
    private boolean allowNativeAccess = true;
    private PolyglotAccess polyglotAccess = PolyglotAccess.ALL;

    public Builder(Language language) {
      this.language = language;
    }

    public Builder allowExperimentalOptions(boolean v) {
      allowExperimentalOptions = v;
      return this;
    }

    public Builder allowAllAccess(boolean v) {
      allowAllAccess = v;
      return this;
    }

    public Builder hostAccess(HostAccess v) {
      hostAccess = v;
      return this;
    }

    public Builder ioAccess(IOAccess v) {
      ioAccess = v;
      return this;
    }

    public Builder allowCreateThread(boolean v) {
      allowCreateThread = v;
      return this;
    }

    public Builder allowNativeAccess(boolean v) {
      allowNativeAccess = v;
      return this;
    }

    public Builder polyglotAccess(PolyglotAccess v) {
      polyglotAccess = v;
      return this;
    }

    public Context build() {
      return Context.newBuilder(language.id())
          .allowExperimentalOptions(allowExperimentalOptions)
          .allowAllAccess(allowAllAccess)
          .allowHostAccess(hostAccess)
          .allowIO(ioAccess)
          .allowCreateThread(allowCreateThread)
          .allowNativeAccess(allowNativeAccess)
          .allowPolyglotAccess(polyglotAccess)
          .build();
    }
  }
}
