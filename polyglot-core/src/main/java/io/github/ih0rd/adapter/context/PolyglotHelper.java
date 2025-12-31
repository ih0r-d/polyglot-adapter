package io.github.ih0rd.adapter.context;

import java.util.Objects;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

/// # PolyglotHelper
///
/// Lightweight helper for creating and initializing GraalVM {@link Context} instances.
///
/// This class encapsulates language-specific context configuration and ensures consistent
// initialization across
/// executors.
///
/// Responsibilities:
/// - Selects the appropriate {@link Context.Builder} for a given {@link SupportedLanguage}
/// - Applies optional user-provided builder customization
/// - Initializes the created context for the selected language
///
/// Design notes:
/// - This helper is intentionally minimal and not extensible
/// - It does not handle script loading or execution concerns
/// - Contexts are created with full access enabled by default
///
/// NOTE: Contexts are created with {@code allowAllAccess(true)} enabled. This is a deliberate
// choice to favor
/// simplicity and interoperability. Access restrictions may be introduced in future releases.
///
public final class PolyglotHelper {

  private static final String OPTION_FALSE = "false";

  private static final String ENGINE_WARN_INTERPRETER_ONLY = "engine.WarnInterpreterOnly";

  private static final String PYTHON_WARN_EXPERIMENTAL_FEATURES = "python.WarnExperimentalFeatures";

  private PolyglotHelper() {}

  /// ### newContext
  ///
  /// Creates and initializes a new {@link Context} for the given language.
  ///
  /// An optional {@link Consumer} may be provided to customize the {@link Context.Builder} before
  // the context is
  /// built.
  ///
  /// ```java
  /// Context ctx = PolyglotHelper.newContext(
  ///     SupportedLanguage.PYTHON,
  ///     b -> b.option("engine.WarnInterpreterOnly", "false")
  /// );
  /// ```
  ///
  /// @param language   guest language (e.g. {@code PYTHON}, {@code JS})
  /// @param customizer optional context builder customizer, may be {@code null}
  /// @return initialized {@link Context}
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
                .allowExperimentalOptions(true)
                .option(ENGINE_WARN_INTERPRETER_ONLY, OPTION_FALSE)
                .option(PYTHON_WARN_EXPERIMENTAL_FEATURES, OPTION_FALSE);
      }

      case JS -> {
        builder =
            Context.newBuilder(language.id())
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                .option(ENGINE_WARN_INTERPRETER_ONLY, OPTION_FALSE);
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
  /// Creates and initializes a new {@link Context} with default configuration for the given
  // language.
  ///
  /// @param language guest language
  /// @return initialized {@link Context}
  public static Context newContext(SupportedLanguage language) {
    return newContext(language, null);
  }
}
