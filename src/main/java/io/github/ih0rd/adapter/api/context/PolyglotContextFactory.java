package io.github.ih0rd.adapter.api.context;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

/// # PolyglotContextFactory
///
/// Factory utility for building and configuring GraalVM `Context` instances.
///
/// ---
/// ## Overview
/// Provides a flexible builder API for creating isolated or shared polyglot runtime contexts for
/// multiple languages
/// (e.g., **Python**, **JavaScript**).
///
/// Supports:
/// - Safe defaults for GraalPy (`withSafePythonDefaults()`).
/// - Node.js compatibility for GraalJS (`withNodeSupport()`).
/// - User-defined customization via `apply(Consumer<Context.Builder>)`.
/// - Extensible HostAccess mappings (`extendHostAccess()`).
/// - Virtual File System setup for resource loading.
///
/// ---
/// ## Example
/// ```java
/// var ctx = new PolyglotContextFactory.Builder(SupportedLanguage.PYTHON)
///     .withSafePythonDefaults()
///     .extendHostAccess(b -> b.targetTypeMapping(
///         Value.class, java.time.Duration.class,
///         Value::isString, v -> java.time.Duration.parse(v.asString())
/// ))
///     .apply(b -> b.option("python.VerboseFlag", "true"))
///     .build();
/// ```
///
/// @since 0.0.16 Author: ih0r-d
public final class PolyglotContextFactory {

  private PolyglotContextFactory() {}

  /// ### createDefault
  /// Creates a default polyglot {@link Context} for the given language.
  public static Context createDefault(SupportedLanguage language) {
    return new Builder(language).build();
  }

  /// # Builder
  ///
  /// Fluent builder for GraalVM {@link Context} configuration.
  public static final class Builder {

    private final SupportedLanguage language;
    private Path resourcesPath;
    private final Map<String, String> customOptions = new HashMap<>();

    private Consumer<Context.Builder> userCustomizer = b -> {};
    private Consumer<HostAccess.Builder> hostAccessExtender = b -> {};
    private HostAccess userHostAccess = HostAccess.ALL;

    private boolean enableSafePythonDefaults = false;
    private boolean enableNodeSupport = false;
    private String resourceDirectory = "org.graalvm.python.vfs";

    public Builder(SupportedLanguage language) {
      this.language = language;
      this.resourcesPath = ResourcesProvider.get(language);
    }

    /// ### apply
    /// Adds a custom modifier for {@link Context.Builder}.
    public Builder apply(Consumer<Context.Builder> customizer) {
      if (customizer != null) {
        this.userCustomizer = this.userCustomizer.andThen(customizer);
      }
      return this;
    }

    /// ### hostAccess
    /// Replaces the base {@link HostAccess} used by this context.
    public Builder hostAccess(HostAccess access) {
      if (access != null) {
        this.userHostAccess = access;
      }
      return this;
    }

    /// ### extendHostAccess
    /// Adds custom type mappings or access rules.
    ///
    /// Example:
    /// ```java
    /// .extendHostAccess(b -> b.targetTypeMapping(
    ///     Value.class, Path.class, Value::isString, v -> Path.of(v.asString())
    /// ));
    /// ```
    public Builder extendHostAccess(Consumer<HostAccess.Builder> extender) {
      if (extender != null) {
        this.hostAccessExtender = this.hostAccessExtender.andThen(extender);
      }
      return this;
    }

    /// ### option
    /// Adds a single GraalVM engine option.
    public Builder option(String key, String value) {
      if (key != null && value != null) {
        customOptions.put(key, value);
      }
      return this;
    }

    /// ### options
    /// Adds multiple engine options.
    public Builder options(Map<String, String> options) {
      if (options != null) {
        customOptions.putAll(options);
      }
      return this;
    }

    /// ### withSafePythonDefaults
    /// Enables recommended defaults for GraalPy.
    public Builder withSafePythonDefaults() {
      this.enableSafePythonDefaults = true;
      return this;
    }

    /// ### withNodeSupport
    /// Enables Node.js compatibility for GraalJS.
    public Builder withNodeSupport() {
      this.enableNodeSupport = true;
      return this;
    }

    /// ### resourcesPath
    /// Sets a filesystem path for resources.
    public Builder resourcesPath(Path path) {
      this.resourcesPath = path;
      return this;
    }

    public Path getResourcesPath() {
      return resourcesPath;
    }

    /// ### resourceDirectory
    /// Sets the virtual directory for GraalPy VFS.
    public Builder resourceDirectory(String dir) {
      if (dir != null && !dir.isBlank()) {
        this.resourceDirectory = dir;
      }
      return this;
    }

    /// ### build
    /// Constructs and initializes a GraalVM {@link Context} instance with full interop and
    // experimental options
    /// enabled by default.
    ///
    /// This configuration follows Oracle's embedding guidelines:
    /// - full host access for seamless Java ↔ guest interoperability
    /// - user-extendable HostAccess mappings via extendHostAccess(...)
    /// - SDK-provided default mappings with LOW precedence
    public Context build() {
      Context.Builder builder;

      switch (language) {
        case PYTHON -> {
          var vfs = VirtualFileSystem.newBuilder().resourceDirectory(resourceDirectory).build();

          var pyBuilder =
              GraalPyResources.contextBuilder(vfs)
                  .allowAllAccess(true)
                  .allowExperimentalOptions(true);

          if (enableSafePythonDefaults) {
            pyBuilder
                .option("engine.WarnInterpreterOnly", "false")
                .option("python.WarnExperimentalFeatures", "false")
                .option("python.CAPI", "false")
                .option("python.VerboseFlag", "false")
                .option("log.file", "/dev/null");
          }

          customOptions.forEach(pyBuilder::option);
          builder = pyBuilder;
        }

        case JS -> {
          var jsBuilder =
              Context.newBuilder(language.id())
                  .allowAllAccess(true)
                  .allowExperimentalOptions(true)
                  .option("engine.WarnInterpreterOnly", "false");

          if (enableNodeSupport) {
            jsBuilder.option("js.ecmascript-version", "latest").option("js.console", "true");
          }

          customOptions.forEach(jsBuilder::option);
          builder = jsBuilder;
        }

        default -> throw new IllegalStateException("Unsupported language: " + language);
      }

      HostAccess.Builder hostAccessBuilder = HostAccess.newBuilder(userHostAccess);

      hostAccessBuilder.targetTypeMapping(
          Value.class,
          Path.class,
          Value::isString,
          v -> Path.of(v.asString()),
          HostAccess.TargetMappingPrecedence.LOW);

      hostAccessExtender.accept(hostAccessBuilder);

      builder.allowHostAccess(hostAccessBuilder.build());
      builder.apply(userCustomizer);

      Context ctx = builder.build();
      ctx.initialize(language.id());
      return ctx;
    }
  }
}
