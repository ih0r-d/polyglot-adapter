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

import io.github.ih0rd.adapter.context.JsExecutor;
import io.github.ih0rd.adapter.context.SupportedLanguage;
import io.github.ih0rd.polyglot.spring.context.SpringPolyglotContextFactory;
import io.github.ih0rd.polyglot.spring.internal.PolyglotWarmupConstants;
import io.github.ih0rd.polyglot.spring.properties.PolyglotProperties;

@AutoConfiguration
@ConditionalOnClass(JsExecutor.class)
@ConditionalOnProperty(prefix = "polyglot.js", name = "enabled", havingValue = "true")
public class PolyglotJsAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(PolyglotJsAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  public JsExecutor jsExecutor(
      PolyglotProperties props, SpringPolyglotContextFactory contextFactory) {
    Context context = contextFactory.create(SupportedLanguage.JS);
    var resources = props.js().resourcesPath() != null ? Path.of(props.js().resourcesPath()) : null;
    return new JsExecutor(context, resources);
  }

  @Bean
  @ConditionalOnProperty(prefix = "polyglot.js", name = "warmup-on-startup", havingValue = "true")
  public ApplicationListener<ApplicationReadyEvent> jsWarmup(
      JsExecutor executor, PolyglotProperties props) {
    return _ -> {
      try {
        executor.evaluate(PolyglotWarmupConstants.NOOP_EXPRESSION);
      } catch (Exception ex) {
        if (props.core().failFast()) {
          throw ex;
        }
        log.warn("JS warmup failed", ex);
      }
    };
  }
}
