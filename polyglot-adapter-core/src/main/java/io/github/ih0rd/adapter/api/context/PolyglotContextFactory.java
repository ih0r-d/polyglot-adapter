package io.github.ih0rd.adapter.api.context;

import java.nio.file.Path;

import io.github.ih0rd.adapter.exceptions.EvaluationException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

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

        public Builder(Language language) {
            this.language = language;
            this.resourcesPath = ResourcesProvider.get(language);
        }

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
                    yield pyBuilder;
                }
                case JS -> Context.newBuilder(language.id());
                case null -> throw new EvaluationException("Unknown language " + language);
            };
            return builder.allowExperimentalOptions(allowExperimentalOptions)
                    .allowAllAccess(allowAllAccess)
                    .allowHostAccess(hostAccess)
                    .allowCreateThread(allowCreateThread)
                    .allowNativeAccess(allowNativeAccess)
                    .allowPolyglotAccess(polyglotAccess)
                    .build();
        }
    }
}
