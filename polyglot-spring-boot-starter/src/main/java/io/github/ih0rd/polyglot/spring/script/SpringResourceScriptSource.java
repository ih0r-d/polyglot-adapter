package io.github.ih0rd.polyglot.spring.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import io.github.ih0rd.adapter.context.SupportedLanguage;
import io.github.ih0rd.adapter.script.ScriptSource;

/// # SpringResourceScriptSource
///
/// Spring-backed implementation of {@link ScriptSource}, bound to a **single language**.
///
/// ## Purpose
/// Resolve polyglot scripts using Spring's {@link ResourceLoader},
/// allowing usage of any Spring-supported resource location:
///
/// - `classpath:/python/`
/// - `classpath:/js/`
/// - `file:./scripts/`
/// - `file:/absolute/path/`
///
/// ## Design principles
/// - **One ScriptSource per language**
/// - No filesystem assumptions
/// - No Spring dependencies leaked into core
/// - Stateless and thread-safe
///
/// ## Responsibility
/// - Resolve script existence
/// - Open scripts as UTF-8 {@link Reader}
/// - Map logical script names to Spring resources
///
/// ## Non-goals
/// - Script caching
/// - Script execution
/// - Hot reload
///
public final class SpringResourceScriptSource implements ScriptSource {

    private final ResourceLoader resourceLoader;
    private final SupportedLanguage language;
    private final String basePath;

    /// ## Constructor
    ///
    /// @param resourceLoader Spring {@link ResourceLoader}
    /// @param language polyglot language this source is bound to
    /// @param basePath base resource location (must end with `/` or be normalizable)
    public SpringResourceScriptSource(
            ResourceLoader resourceLoader,
            SupportedLanguage language,
            String basePath) {

        if (resourceLoader == null) {
            throw new IllegalArgumentException("ResourceLoader must not be null");
        }
        if (language == null) {
            throw new IllegalArgumentException("SupportedLanguage must not be null");
        }

        this.resourceLoader = resourceLoader;
        this.language = language;
        this.basePath = normalizeBase(basePath);
    }

    /// ## exists
    ///
    /// Checks whether a script exists for the given logical name.
    ///
    /// @param language requested language
    /// @param scriptName logical script name (without extension)
    /// @return {@code true} if the resource exists and language matches
    @Override
    public boolean exists(SupportedLanguage language, String scriptName) {
        return this.language == language && resolve(scriptName).exists();
    }

    /// ## open
    ///
    /// Opens the script as a UTF-8 {@link Reader}.
    ///
    /// @param language requested language
    /// @param scriptName logical script name (without extension)
    /// @return reader for the script content
    /// @throws IOException if resource cannot be read
    @Override
    public Reader open(SupportedLanguage language, String scriptName)
            throws IOException {

        if (this.language != language) {
            throw new IllegalArgumentException(
                    "ScriptSource bound to " + this.language + ", but requested " + language);
        }

        return new InputStreamReader(
                resolve(scriptName).getInputStream(),
                StandardCharsets.UTF_8
        );
    }

    /// ## resolve
    ///
    /// Resolves a Spring {@link Resource} for the given script name.
    private Resource resolve(String scriptName) {
        return resourceLoader.getResource(
                basePath + scriptName + language.ext()
        );
    }

    /// ## normalizeBase
    ///
    /// Ensures base path always ends with `/`.
    private static String normalizeBase(String base) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException(
                    "Script base location must not be null or blank");
        }
        return base.endsWith("/") ? base : base + "/";
    }
}
