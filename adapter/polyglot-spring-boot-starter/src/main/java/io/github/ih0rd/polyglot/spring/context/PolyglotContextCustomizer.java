package io.github.ih0rd.polyglot.spring.context;

import org.graalvm.polyglot.Context;
import org.springframework.core.Ordered;

import io.github.ih0rd.contract.SupportedLanguage;

/// Allows user to customize GraalVM Context.Builder before executor creation.
public interface PolyglotContextCustomizer extends Ordered {

  void customize(SupportedLanguage language, Context.Builder builder);

  @Override
  default int getOrder() {
    return 0;
  }
}
