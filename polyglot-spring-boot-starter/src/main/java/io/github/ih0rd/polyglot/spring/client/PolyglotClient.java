package io.github.ih0rd.polyglot.spring.client;

import java.lang.annotation.*;

import org.springframework.stereotype.Component;

import io.github.ih0rd.contract.SupportedLanguage;

/// Marks an interface as a polyglot-backed client.
///
/// Language resolution rules:
/// - if {@link #languages()} is empty:
///   - exactly one executor must be available
/// - if one language is specified:
///   - that executor is used
/// - if multiple languages are specified:
///   - startup fails
///
/// Binding follows {@link Convention#DEFAULT}.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface PolyglotClient {

  /// Allowed guest languages for this client.
  ///
  /// An empty array means automatic resolution based on
  /// available executors.
  SupportedLanguage[] languages() default {};

  /// Binding convention used by the adapter.
  Convention convention() default Convention.DEFAULT;
}
