package io.github.ih0rd.adapter.api.context;

import java.nio.file.Path;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.io.IOAccess;

/**
 * Factory for building configured {@link Context} instances. Supports language-specific resource
 * paths (Python, JS, etc.) resolved via {@link ResourcesProvider}.
 */
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
    private Path resourcesPath;

    public Builder(Language language) {
      this.language = language;
      this.resourcesPath = ResourcesProvider.get(language);
    }

    /** Override resource path for the current language. */
    public Builder resourcesPath(Path path) {
      this.resourcesPath = path;
      return this;
    }

    public Path getResourcesPath() {
      return resourcesPath;
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
