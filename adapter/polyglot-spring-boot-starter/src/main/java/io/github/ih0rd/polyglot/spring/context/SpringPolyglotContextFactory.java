package io.github.ih0rd.polyglot.spring.context;

import org.graalvm.polyglot.Context;
import org.springframework.beans.factory.ObjectProvider;

import io.github.ih0rd.adapter.context.PolyglotHelper;
import io.github.ih0rd.contract.SupportedLanguage;

public final class SpringPolyglotContextFactory {

  private final ObjectProvider<PolyglotContextCustomizer> customizers;

  public SpringPolyglotContextFactory(ObjectProvider<PolyglotContextCustomizer> customizers) {
    this.customizers = customizers;
  }

  public Context create(SupportedLanguage language) {
    return PolyglotHelper.newContext(
        language,
        builder -> customizers.orderedStream().forEach(c -> c.customize(language, builder)));
  }
}
