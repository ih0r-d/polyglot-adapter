package io.github.ih0rd.adapter.api.context;

import java.nio.file.Path;

import io.github.ih0rd.adapter.exceptions.EvaluationException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

/**
 * Factory for creating Polyglot {@link Context} instances
 * with configurable access and embedded resource management.
 *
 * <p>Provides builder-based configuration for:
 * <ul>
 *     <li>Host/native access and experimental options</li>
 *     <li>Custom resource paths (VFS)</li>
 *     <li>Optional safe defaults for GraalPy (suppressing noisy logs & CAPI issues)</li>
 * </ul>
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
        private boolean allowCreateThread = true;
        private boolean allowNativeAccess = true;
        private PolyglotAccess polyglotAccess = PolyglotAccess.ALL;
        private String resourceDirectory = "org.graalvm.python.vfs";
        private Path resourcesPath;

        // internal flag
        private boolean applySafePythonDefaults = false;

        public Builder(Language language) {
            this.language = language;
            this.resourcesPath = ResourcesProvider.get(language);
        }

        // ───────────────────────────────
        // Standard configuration methods
        // ───────────────────────────────
        public Builder resourceDirectory(String resourceDir) {
            if (resourceDir != null && !resourceDir.isBlank()) {
                this.resourceDirectory = resourceDir;
            }
            return this;
        }

        public Builder resourcesPath(Path path) {
            this.resourcesPath = path;
            return this;
        }

        public Path getResourcesPath() {
            return resourcesPath;
        }

        public Builder allowExperimentalOptions(boolean v) { allowExperimentalOptions = v; return this; }
        public Builder allowAllAccess(boolean v) { allowAllAccess = v; return this; }
        public Builder hostAccess(HostAccess v) { hostAccess = v; return this; }
        public Builder allowCreateThread(boolean v) { allowCreateThread = v; return this; }
        public Builder allowNativeAccess(boolean v) { allowNativeAccess = v; return this; }
        public Builder polyglotAccess(PolyglotAccess v) { polyglotAccess = v; return this; }

        // ───────────────────────────────
        // New feature: safe defaults for GraalPy
        // ───────────────────────────────
        /**
         * Applies a pre-configured set of options to suppress noisy warnings,
         * disable problematic C extensions, and redirect internal logs.
         *
         * <p>This is especially useful on macOS ARM (M1/M2/M3),
         * where GraalPy’s NumPy C API causes Mach-O modification errors.
         */
        public Builder withSafePythonDefaults() {
            this.applySafePythonDefaults = true;
            return this;
        }

        // ───────────────────────────────
        // Build the actual Polyglot Context
        // ───────────────────────────────
        public Context build() {
            var vfs = VirtualFileSystem.newBuilder()
                    .resourceDirectory(resourceDirectory)
                    .build();

            var builder = switch (language) {
                case PYTHON -> {
                    var pyBuilder = GraalPyResources.contextBuilder(vfs);
                    if (allowExperimentalOptions) {
                        pyBuilder.option("python.IsolateNativeModules", "true");
                    }

                    // Apply safe defaults if enabled
                    if (applySafePythonDefaults) {
                        pyBuilder
                                .option("python.WarnExperimentalFeatures", "false")
                                .option("engine.WarnInterpreterOnly", "false")
                                .option("log.file", "/dev/null")
                                .option("python.CAPI", "false");
                    }
                    yield pyBuilder;
                }
                case JS -> Context.newBuilder(language.id());
                case null -> throw new EvaluationException("Unknown language " + language);
            };

            return builder
                    .allowExperimentalOptions(allowExperimentalOptions)
                    .allowAllAccess(allowAllAccess)
                    .allowHostAccess(hostAccess)
                    .allowCreateThread(allowCreateThread)
                    .allowNativeAccess(allowNativeAccess)
                    .allowPolyglotAccess(polyglotAccess)
                    .build();
        }
    }
}
