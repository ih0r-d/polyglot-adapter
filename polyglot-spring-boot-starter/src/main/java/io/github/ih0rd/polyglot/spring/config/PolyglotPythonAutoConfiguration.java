package io.github.ih0rd.polyglot.spring.config;

import org.graalvm.polyglot.Context;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.adapter.context.SupportedLanguage;
import io.github.ih0rd.adapter.script.ScriptSource;
import io.github.ih0rd.polyglot.spring.context.SpringPolyglotContextFactory;
import io.github.ih0rd.polyglot.spring.properties.PolyglotProperties;
import io.github.ih0rd.polyglot.spring.script.SpringResourceScriptSource;

/// # PolyglotPythonAutoConfiguration
///
/// Spring Boot autoconfiguration for {@link PyExecutor}.
///
/// Responsibilities:
/// - Create language-bound {@link ScriptSource} for Python
/// - Create {@link PyExecutor} with externally managed {@link Context}
/// - Delegate warmup and lifecycle handling to internal components
///
@AutoConfiguration
@ConditionalOnClass(PyExecutor.class)
@ConditionalOnProperty(prefix = "polyglot.python", name = "enabled", havingValue = "true")
public class PolyglotPythonAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "pyScriptSource")
  public ScriptSource pyScriptSource(ResourceLoader resourceLoader, PolyglotProperties properties) {

    return new SpringResourceScriptSource(
        resourceLoader, SupportedLanguage.PYTHON, properties.python().resourcesPath());
  }

  @Bean
  @ConditionalOnMissingBean
  public PyExecutor pyExecutor(
      SpringPolyglotContextFactory contextFactory, ScriptSource pyScriptSource) {

    Context context = contextFactory.create(SupportedLanguage.PYTHON);
    return new PyExecutor(context, pyScriptSource);
  }
}
