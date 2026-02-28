package io.github.ih0rd.contract;

/// # LanguageAstParser
///
/// SPI for language-specific AST parsers.
///
/// Implementations are responsible for parsing source code
/// of a specific language into a {@link ContractModel}.
///
public interface LanguageParser {

    /// @return supported language
    SupportedLanguage language();

    /// Parses script into contract model.
    ContractModel parse(ScriptDescriptor script, CodegenConfig config);
}
