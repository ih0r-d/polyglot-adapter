package io.github.ih0rd.adapter.api.context;

import java.nio.file.Path;
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
/// This helper centralizes context creation for GraalVM-supported languages (Python, JS, etc.).
/// It provides both **embedded (VFS)** and **filesystem-based** execution modes.
///
/// - Embedded mode uses `org.graalvm.python.vfs` (default GraalPy runtime assets)
/// - Filesystem mode allows loading scripts from directories defined via
///   system properties (`py.polyglot-resources.path` / `js.polyglot-resources.path`)
///
/// ---
/// ## Usage
/// ```java
/// // Default embedded mode
/// Context ctx = PolyglotHelper.createPythonContext(true);
///
/// // Filesystem-based mode
/// Context ctx = PolyglotHelper.createPythonFsContext(true);
/// ```
///
/// Both modes apply the same safe defaults and host access configuration.
public final class PolyglotHelper {

    private PolyglotHelper() {}

    /// ### createPythonContext
    /// Creates a Python context using **embedded GraalPy VFS** (default mode).
    public static Context createPythonContext(boolean safeDefaults) {
        return createPythonContextInternal(safeDefaults, false);
    }

    /// ### createPythonFsContext
    /// Creates a Python context loading modules and scripts from **filesystem**.
    public static Context createPythonFsContext(boolean safeDefaults) {
        return createPythonContextInternal(safeDefaults, true);
    }

    /// ### createPythonContextInternal
    /// Internal shared logic for both embedded and filesystem context creation.
    private static Context createPythonContextInternal(boolean safeDefaults, boolean useFilesystem) {
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

        var ctx = builder.build();
        ctx.initialize("python");
        return ctx;
    }

    /// ### createJsContext
    /// Creates a JavaScript context with optional Node.js support.
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
