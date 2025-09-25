package io.github.ih0r.adapter.api.context;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

import java.nio.file.Path;

public final class PolyglotContextFactory {

    private PolyglotContextFactory() {
    }

    public static Context createDefault() {
        return new Builder().build();
    }

    public static Context createFromResourceDir(Path resourceDir) {
        return new Builder().resourceDir(resourceDir).build();
    }

    public static final class Builder {
        private boolean allowExperimentalOptions = false;
        private boolean allowAllAccess = true;
        private HostAccess hostAccess = HostAccess.ALL;
        private IOAccess ioAccess = IOAccess.ALL;
        private boolean allowCreateThread = true;
        private boolean allowNativeAccess = true;
        private PolyglotAccess polyglotAccess = PolyglotAccess.ALL;
        private Path resourceDir;

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

        public Builder resourceDir(Path v) {
            resourceDir = v;
            return this;
        }

        public Context build() {
            var builder = (resourceDir != null)
                    ? GraalPyResources.contextBuilder(resourceDir)
                    : GraalPyResources.contextBuilder(VirtualFileSystem.newBuilder().build());

            return builder
                    .option("python.PythonHome", "")
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
