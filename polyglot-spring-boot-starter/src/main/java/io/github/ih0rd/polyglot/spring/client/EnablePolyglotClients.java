package io.github.ih0rd.polyglot.spring.client;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;

/// Enables scanning for interfaces annotated with {@code @PolyglotClient}.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PolyglotClientRegistrar.class)
public @interface EnablePolyglotClients {

  /// Base packages to scan.
  String[] basePackages() default {};
}
