package io.github.ih0rd.adapter.api.context;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

/// # PolyglotContextFactory
///
/// Factory utility for building and configuring GraalVM `Context` instances.
///
/// ---
/// ## Overview
/// Provides a flexible builder API for creating isolated or shared
/// polyglot runtime contexts for multiple languages (e.g., **Python**, **JavaScript**).
///
/// Supports:
/// - Safe default options for GraalPy (`withSafePythonDefaults()`).
/// - Node.js compatibility mode for GraalJS (`withNodeSupport()`).
/// - User-defined custom engine options (`option()` / `options()`).
/// - Virtual File System configuration for embedded resource loading.
///
/// ---
/// ## Example
/// ```java
/// var ctx = new PolyglotContextFactory.Builder(Language.PYTHON)
///     .withSafePythonDefaults()
///     .allowAllAccess(true)
///     .option("python.CAPI", "false")
///     .build();
/// ```
///
/// ---
/// ## Thread safety
/// - Builder is **not thread-safe** (per-instance configuration only).
/// - Created `Context` objects are thread-confined (one thread at a time).
///
/// ---
/// ## Typical usage
/// ```java
/// try (var executor = PyExecutor.createDefault()) {
///     // The context is automatically managed via BaseExecutor
/// }
/// ```
///
/// @since 0.0.12
/// @author ih0rd
public final class PolyglotContextFactory {

  private PolyglotContextFactory() {}

  /// ### createDefault
  /// Creates a default polyglot {@link Context} for the specified language.
  ///
  /// #### Example
  /// ```java
  /// var ctx = PolyglotContextFactory.createDefault(Language.PYTHON);
  /// ```
  public static Context createDefault(Language language) {
    return new Builder(language).build();
  }

  /// ### Builder
  /// Fluent builder for GraalVM {@link Context} configuration.
  ///
  /// ---
  /// #### Supported languages
  /// - **Python** (GraalPy)
  /// - **JavaScript** (GraalJS, optional Node.js support)
  ///
  /// #### Example
  /// ```java
  /// var ctx = new PolyglotContextFactory.Builder(Language.JS)
  ///     .withNodeSupport()
  ///     .option("js.console", "true")
  ///     .build();
  /// ```
  public static final class Builder {
    private final Language language;
    private boolean allowExperimentalOptions = false;
    private boolean allowAllAccess = true;
    private HostAccess hostAccess = HostAccess.ALL;
    private boolean allowCreateThread = true;
    private boolean allowNativeAccess = true;
    private PolyglotAccess polyglotAccess = PolyglotAccess.ALL;
    private String resourceDirectory = "org.graalvm.python.vfs";
    private Path resourcesPath;

    // Internal flags
    private boolean applySafePythonDefaults = false;
    private boolean enableNodeSupport = false;

    // User-defined engine options
    private final Map<String, String> customOptions = new HashMap<>();

    public Builder(Language language) {
      this.language = language;
      this.resourcesPath = ResourcesProvider.get(language);
    }

    /// ### resourceDirectory
    /// Sets the virtual resource directory used by GraalPy.
    ///
    /// ```java
    /// builder.resourceDirectory("org.graalvm.python.vfs.custom");
    /// ```
    public Builder resourceDirectory(String resourceDir) {
      if (resourceDir != null && !resourceDir.isBlank()) {
        this.resourceDirectory = resourceDir;
      }
      return this;
    }

    /// ### resourcesPath
    /// Sets a filesystem path for language resources.
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

    /// ### withSafePythonDefaults
    /// Enables recommended safe defaults for **GraalPy**:
    /// - Disables C API usage
    /// - Suppresses experimental feature warnings
    /// - Redirects logs to `/dev/null`
    ///
    /// ```java
    /// builder.withSafePythonDefaults();
    /// ```
    public Builder withSafePythonDefaults() {
      this.applySafePythonDefaults = true;
      return this;
    }

    /// ### withNodeSupport
    /// Enables **Node.js** compatibility mode for **GraalJS**.
    ///
    /// Includes:
    /// - `require()` module support
    /// - Built-in `fs`, `path`, and `npm install` availability
    ///
    /// ```java
    /// builder.withNodeSupport();
    /// ```
    public Builder withNodeSupport() {
      this.enableNodeSupport = true;
      return this;
    }

    /// ### option
    /// Adds a single GraalVM engine option.
    ///
    /// ```java
    /// builder.option("python.CAPI", "false");
    /// ```
    public Builder option(String key, String value) {
      if (key != null && value != null) {
        customOptions.put(key, value);
      }
      return this;
    }

    /// ### options
    /// Adds multiple engine options at once.
    ///
    /// ```java
    /// builder.options(Map.of(
    ///     "python.CAPI", "false",
    ///     "log.file", "/dev/null"
    /// ));
    /// ```
    public Builder options(Map<String, String> options) {
      if (options != null) {
        customOptions.putAll(options);
      }
      return this;
    }

    /// ### build
    /// Constructs a configured GraalVM {@link Context} instance.
    ///
    /// Applies:
    /// - VFS setup
    /// - Host and polyglot access rules
    /// - Optional safe defaults
    /// - User-defined engine options
    public Context build() {

      Context.Builder builder =
          switch (language) {
            case PYTHON -> {
              var vfs = VirtualFileSystem.newBuilder().resourceDirectory(resourceDirectory).build();

              var pyBuilder = GraalPyResources.contextBuilder(vfs);
              if (allowExperimentalOptions) {
                pyBuilder.option("python.IsolateNativeModules", "true");
              }
              if (applySafePythonDefaults) {
                pyBuilder
                    .option("python.WarnExperimentalFeatures", "false")
                    .option("engine.WarnInterpreterOnly", "false")
                    .option("log.file", "/dev/null")
                    .option("python.CAPI", "false");
              }

              customOptions.forEach(pyBuilder::option);

              yield pyBuilder
                  .allowExperimentalOptions(allowExperimentalOptions)
                  .allowAllAccess(allowAllAccess)
                  .allowHostAccess(hostAccess)
                  .allowCreateThread(allowCreateThread)
                  .allowNativeAccess(allowNativeAccess)
                  .allowPolyglotAccess(polyglotAccess);
            }

            case JS -> {
              var jsBuilder =
                  Context.newBuilder(language.id())
                      .allowExperimentalOptions(allowExperimentalOptions)
                      .allowAllAccess(allowAllAccess)
                      .allowHostAccess(hostAccess)
                      .allowCreateThread(allowCreateThread)
                      .allowNativeAccess(allowNativeAccess)
                      .allowPolyglotAccess(polyglotAccess);

              if (enableNodeSupport) {
                jsBuilder
                    .option("js.node", "true")
                    .option("js.ecmascript-version", "latest")
                    .option("js.console", "true");
              }

              customOptions.forEach(jsBuilder::option);

              yield jsBuilder;
            }
          };
      Context ctx = builder.build();
      ctx.initialize(language.id());
      return ctx;
    }
  }
}
