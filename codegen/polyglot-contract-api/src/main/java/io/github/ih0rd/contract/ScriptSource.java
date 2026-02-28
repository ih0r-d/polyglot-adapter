package io.github.ih0rd.contract;

import java.io.IOException;
import java.io.Reader;

/// # ScriptSource
///
/// Abstraction for resolving and opening polyglot scripts.
///
/// This SPI defines *where scripts come from*, without imposing
/// any assumptions about filesystem layout, classpath usage,
/// environment variables, or runtime configuration.
///
/// Core executors rely exclusively on this contract and must not
/// perform any direct I/O operations themselves.
///
/// ---
///
/// ## Responsibilities:
/// - Resolves scripts by logical name and language
/// - Provides a {@link Reader} for script content
/// - Encapsulates all script I/O concerns outside the core execution flow
///
/// ## Design notes:
/// - Implementations may load scripts from filesystem, classpath, memory, or remote sources
/// - Script naming and extension handling are implementation details
/// - Implementations are expected to be deterministic and side-effect free
///
/// ## Error handling:
/// - {@link #exists(SupportedLanguage, String)} should return {@code false}
///   if the script cannot be resolved
/// - {@link #open(SupportedLanguage, String)} should throw {@link IOException}
///   only for I/O-related failures, not for missing scripts
///
public interface ScriptSource {

  /// Checks whether a script with the given logical name exists
  /// for the specified language.
  ///
  /// @param language   target script language
  /// @param scriptName logical script name (without extension assumptions)
  /// @return {@code true} if the script can be resolved, {@code false} otherwise
  boolean exists(SupportedLanguage language, String scriptName);

  /// Opens a {@link Reader} for the specified script.
  ///
  /// Implementations must return a fresh {@link Reader} instance
  /// for each invocation.
  ///
  /// @param language   target script language
  /// @param scriptName logical script name
  /// @return reader for the script content
  /// @throws IOException if an I/O error occurs while opening the script
  Reader open(SupportedLanguage language, String scriptName) throws IOException;
}
