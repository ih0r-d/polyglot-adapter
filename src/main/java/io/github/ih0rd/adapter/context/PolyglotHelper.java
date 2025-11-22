package io.github.ih0rd.adapter.context;

import java.util.Objects;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

/// # PolyglotHelper
///
/// Lightweight helper for creating GraalVM {@link Context} instances with optional builder
// customization.
///
/// Responsibilities:
/// - Selects the proper builder for a given {@link SupportedLanguage}
/// - Applies a user-provided {@link Consumer} to the {@link Context.Builder}
/// - Initializes the created context for the selected language
public final class PolyglotHelper {

  private PolyglotHelper() {}

  /// ### newContext
  ///
  /// Creates a new {@link Context} for the given language and applies the provided builder
  // customizer (if not
  /// {@code null}).
  ///
  /// ```java
  /// Context ctx = PolyglotHelper.newContext(
  ///     SupportedLanguage.PYTHON,
  ///     b -> b.option("engine.WarnInterpreterOnly", "false")
  /// );
  /// ```
  ///
  /// @param language   guest language (e.g. {@code PYTHON}, {@code JS})
  /// @param customizer optional builder customizer, may be {@code null}
  /// @return configured {@link Context}
  public static Context newContext(
      SupportedLanguage language, Consumer<Context.Builder> customizer) {

    Objects.requireNonNull(language, "language must not be null");

    Context.Builder builder;
    switch (language) {
      case PYTHON -> {
        VirtualFileSystem vfs =
            VirtualFileSystem.newBuilder().resourceDirectory("org.graalvm.python.vfs").build();

        builder =
            GraalPyResources.contextBuilder(vfs)
                .allowAllAccess(true)
                .allowExperimentalOptions(true);
      }
      case JS -> {
        builder =
            Context.newBuilder(language.id()).allowAllAccess(true).allowExperimentalOptions(true);
      }
      default -> throw new IllegalStateException("Unsupported language: " + language);
    }

    if (customizer != null) {
      customizer.accept(builder);
    }

    Context context = builder.build();
    context.initialize(language.id());
    return context;
  }

  /// ### newContext
  ///
  /// Creates a new {@link Context} with default configuration for the given language.
  ///
  /// @param language guest language
  /// @return configured {@link Context}
  public static Context newContext(SupportedLanguage language) {
    return newContext(language, null);
  }
}
