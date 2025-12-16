package io.github.ih0rd.polyglot.spring.config;

import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;

import io.github.ih0rd.adapter.context.JsExecutor;
import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.adapter.context.SupportedLanguage;
import io.github.ih0rd.adapter.utils.Constants;
import io.github.ih0rd.polyglot.spring.PolyglotExecutors;
import io.github.ih0rd.polyglot.spring.context.PolyglotContextCustomizer;
import io.github.ih0rd.polyglot.spring.context.SpringPolyglotContextFactory;
import io.github.ih0rd.polyglot.spring.properties.PolyglotProperties;

@AutoConfiguration
@EnableConfigurationProperties(PolyglotProperties.class)
@ConditionalOnProperty(
    prefix = "polyglot.core",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PolyglotAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(PolyglotAutoConfiguration.class);

  private final PolyglotProperties properties;

  public PolyglotAutoConfiguration(PolyglotProperties properties) {
    this.properties = properties;
    applyResourceSystemProperties(properties);
    validateRuntimePresence(properties);
  }

  /// Applies resource locations as system properties understood by polyglot-adapter.
  ///
  /// - Python: {@code py.polyglot-resources.path}
  /// - JS:     {@code js.polyglot-resources.path}
  private static void applyResourceSystemProperties(PolyglotProperties props) {
    if (props.python().resourcesPath() != null) {
      System.setProperty(Constants.PROP_PY_RESOURCES, props.python().resourcesPath());
    }
    if (props.js().resourcesPath() != null) {
      System.setProperty(Constants.PROP_JS_RESOURCES, props.js().resourcesPath());
    }
  }

  /// Validates that enabled languages are actually available in the runtime.
  ///
  /// Important:
  /// - Do NOT check random internal classes like {@code org.graalvm.python.PythonLanguage}.
  /// - The only reliable check is to ask GraalVM to create a Context for that language id.
  private void validateRuntimePresence(PolyglotProperties props) {
    if (props.python().enabled()) {
      ensureLanguageAvailable(SupportedLanguage.PYTHON.id(), "GraalPy");
    }
    if (props.js().enabled()) {
      ensureLanguageAvailable(SupportedLanguage.JS.id(), "GraalJS");
    }
  }

  /// Ensures that a given GraalVM language is installed and on the classpath.
  ///
  /// This is runtime-accurate because GraalVM fails fast when the language is missing.
  ///
  /// @param langId    GraalVM language id (e.g. {@code "python"}, {@code "js"})
  /// @param humanName name used in error messages
  private void ensureLanguageAvailable(String langId, String humanName) {
    try (Context ctx = Context.newBuilder(langId).allowAllAccess(true).build()) {
      // Force language initialization
      ctx.getEngine().getLanguages();
    } catch (Throwable ex) {
      failOrWarn(
          humanName
              + " runtime is missing but is enabled. "
              + "Add GraalVM "
              + humanName
              + " dependencies. "
              + "Root cause: "
              + ex.getClass().getSimpleName()
              + ": "
              + ex.getMessage());
    }
  }

  /// Throws if fail-fast is enabled, otherwise logs warning.
  private void failOrWarn(String message) {
    if (properties.core().failFast()) {
      throw new IllegalStateException(message);
    }
    log.warn(message);
  }

  @Bean
  @ConditionalOnMissingBean
  public PolyglotExecutors polyglotExecutors(
      ObjectProvider<PyExecutor> py, ObjectProvider<JsExecutor> js) {
    return new PolyglotExecutors(py.getIfAvailable(), js.getIfAvailable());
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "polyglot.core",
      name = "log-metadata-on-startup",
      havingValue = "true",
      matchIfMissing = true)
  public ApplicationListener<ContextRefreshedEvent> startupLogger(
      PolyglotExecutors executors, PolyglotProperties props) {

    final long startedAt = System.nanoTime();

    return _ -> {
      long startupMs =
          java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

      log.info("---- Polyglot Starter ----------------------------------------");

      log.info(
          "Core        : {}, failFast={}, logLevel={}",
          props.core().enabled() ? "ENABLED" : "DISABLED",
          props.core().failFast(),
          props.core().logLevel().toUpperCase());

      if (props.python().enabled()) {
        boolean available = executors.python().isPresent();
        log.info("Python      : ENABLED ({})", available ? "available" : "missing runtime");
        log.info("  resources : {}", props.python().resourcesPath());
        log.info("  warmup    : {}", props.python().warmupOnStartup());
        log.info(
            "  preload   : {}",
            props.python().preloadScripts().isEmpty() ? "none" : props.python().preloadScripts());

        executors
            .python()
            .ifPresent(
                py -> {
                  Object clients = py.metadata().get("instanceCacheSize");
                  log.info("  clients   : {}", clients != null ? clients : 0);
                });
      } else {
        log.info("Python      : DISABLED");
      }

      log.info("JavaScript  : {}", props.js().enabled() ? "ENABLED" : "DISABLED");

      log.info(
          "Executors   : python={}, js={}",
          executors.isPythonEnabled() ? "ACTIVE" : "OFF",
          executors.isJsEnabled() ? "ACTIVE" : "OFF");

      log.info("Startup     : polyglot={} ms", startupMs);

      log.info("--------------------------------------------------------------");
    };
  }

  @Bean
  @ConditionalOnMissingBean
  public SpringPolyglotContextFactory polyglotContextFactory(
      ObjectProvider<PolyglotContextCustomizer> customizers) {
    return new SpringPolyglotContextFactory(customizers);
  }
}
