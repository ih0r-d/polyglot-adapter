package io.github.ih0rd.polyglot.spring.config;

import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.adapter.context.SupportedLanguage;
import io.github.ih0rd.polyglot.spring.context.SpringPolyglotContextFactory;
import io.github.ih0rd.polyglot.spring.internal.PolyglotWarmupConstants;
import io.github.ih0rd.polyglot.spring.properties.PolyglotProperties;

@AutoConfiguration
@ConditionalOnClass(PyExecutor.class)
@ConditionalOnProperty(prefix = "polyglot.python", name = "enabled", havingValue = "true")
public class PolyglotPythonAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(PolyglotPythonAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  public PyExecutor pyExecutor(
      PolyglotProperties props, SpringPolyglotContextFactory contextFactory) {
    Context context = contextFactory.create(SupportedLanguage.PYTHON);
    var resources =
        props.python().resourcesPath() != null ? Path.of(props.python().resourcesPath()) : null;
    return new PyExecutor(context, resources);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "polyglot.python",
      name = "warmup-on-startup",
      havingValue = "true")
  public ApplicationListener<ApplicationReadyEvent> pythonWarmup(
      PyExecutor executor, PolyglotProperties props) {
    return _ -> {
      long start = System.currentTimeMillis();
      log.info("[Polyglot][PYTHON] Warmup started");

      try {
        executor.evaluate(PolyglotWarmupConstants.NOOP_EXPRESSION);

        if (!props.python().preloadScripts().isEmpty()) {
          log.info("[Polyglot][PYTHON] Preloaded scripts: {}", props.python().preloadScripts());
        }

        log.info("[Polyglot][PYTHON] Warmup completed in {}ms", System.currentTimeMillis() - start);

      } catch (Exception ex) {
        if (props.core().failFast()) {
          log.error("[Polyglot][PYTHON] Warmup failed", ex);
          throw ex;
        }
        log.warn("[Polyglot][PYTHON] Warmup failed", ex);
      }
    };
  }
}
