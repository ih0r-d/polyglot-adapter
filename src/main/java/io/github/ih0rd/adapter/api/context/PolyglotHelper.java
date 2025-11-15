package io.github.ih0rd.adapter.api.context;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

/// # PolyglotHelper
///
/// Utility class providing simplified context creation for GraalVM languages.
///
/// ---
/// ## Overview
/// Centralizes context creation for GraalVM-supported languages (Python, JS, etc.).
/// Supports both **embedded (VFS)** and **filesystem-based** execution modes with
/// optional customization via lambda (`Consumer<Context.Builder>`).
public final class PolyglotHelper {

  private PolyglotHelper() {}

  // ------------------------------------------------------------
  // Python Context Builders
  // ------------------------------------------------------------

  /// ### createPythonContext
  /// Creates a Python context using **embedded GraalPy VFS** (default mode).
  public static Context createPythonContext(boolean safeDefaults) {
    return createPythonContext(safeDefaults, null, false);
  }

  /// ### createPythonFsContext
  /// Creates a Python context loading modules and scripts from **filesystem**.
  public static Context createPythonFsContext(boolean safeDefaults) {
    return createPythonContext(safeDefaults, null, true);
  }

  /// ### createPythonContext (with customizer)
  /// Creates a Python context using **embedded GraalPy VFS** and applies a user-provided
  /// lambda for custom `Context.Builder` configuration.
  ///
  /// ```java
  /// var ctx = PolyglotHelper.createPythonContext(true, b -> {
  ///     b.option("python.CAPI", "true");
  ///     b.allowIO(true);
  /// });
  /// ```
  public static Context createPythonContext(
      boolean safeDefaults, Consumer<Context.Builder> customizer) {
    return createPythonContext(safeDefaults, customizer, false);
  }

  /// ### createPythonFsContext (with customizer)
  /// Same as `createPythonContext` but uses filesystem instead of embedded VFS.
  ///
  /// ```java
  /// var ctx = PolyglotHelper.createPythonFsContext(true, b -> {
  ///     b.option("python.CAPI", "true");
  ///     b.option("python.UseBundledPackages", "true");
  ///     b.allowIO(true);
  /// });
  /// ```
  public static Context createPythonFsContext(
      boolean safeDefaults, Consumer<Context.Builder> customizer) {
    return createPythonContext(safeDefaults, customizer, true);
  }

  /// ### createPythonContext (internal)
  /// Internal shared logic for both embedded and filesystem context creation.
  private static Context createPythonContext(
      boolean safeDefaults, Consumer<Context.Builder> customizer, boolean useFilesystem) {

    var vfsBuilder = VirtualFileSystem.newBuilder();

    if (useFilesystem) {
      Path path = ResourcesProvider.get(SupportedLanguage.PYTHON);
      vfsBuilder.resourceDirectory(path.toString());
    } else {
      vfsBuilder.resourceDirectory("org.graalvm.python.vfs");
    }

    var vfs = vfsBuilder.build();

    var builder =
        GraalPyResources.contextBuilder(vfs)
            .allowAllAccess(true)
            .allowExperimentalOptions(true)
            .apply(b -> b.allowHostAccess(defaultHostAccess()));

    if (safeDefaults) {
      builder.option("engine.WarnInterpreterOnly", "false");
      builder.option("python.WarnExperimentalFeatures", "false");
      builder.option("python.CAPI", "false");
      builder.option("python.VerboseFlag", "false");
      builder.option("log.file", "/dev/null");
    }

    if (customizer != null) {
      customizer.accept(builder);
    }

    var ctx = builder.build();
    ctx.initialize("python");
    return ctx;
  }

  // ------------------------------------------------------------
  // JavaScript Context Builder
  // ------------------------------------------------------------

  /// ### createJsContext
  /// Creates a JavaScript context with optional Node.js support.
  ///
  /// ```java
  /// var ctx = PolyglotHelper.createJsContext(true);
  /// ```
  public static Context createJsContext(boolean nodeSupport) {
    var builder =
        Context.newBuilder("js")
            .allowAllAccess(true)
            .allowExperimentalOptions(true)
            .option("engine.WarnInterpreterOnly", "false")
            .apply(b -> b.allowHostAccess(defaultHostAccess()));

    if (nodeSupport) {
      builder.option("js.ecmascript-version", "latest");
      builder.option("js.console", "true");
    }

    var ctx = builder.build();
    ctx.initialize(SupportedLanguage.JS.id());
    return ctx;
  }

  // ------------------------------------------------------------
  // Host Access Defaults
  // ------------------------------------------------------------

  /// ### defaultHostAccess
  /// Provides default host access mappings used across all language contexts.
  private static HostAccess defaultHostAccess() {
    return HostAccess.newBuilder(HostAccess.ALL)
        .targetTypeMapping(
            Value.class,
            Path.class,
            Value::isString,
            v -> Path.of(v.asString()),
            HostAccess.TargetMappingPrecedence.LOW)
        .build();
  }
}
