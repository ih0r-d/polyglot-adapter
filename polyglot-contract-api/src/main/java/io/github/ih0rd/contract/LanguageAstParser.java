package io.github.ih0rd.contract;

import io.github.ih0rd.contract.CodegenConfig;
import io.github.ih0rd.contract.ScriptDescriptor;
import io.github.ih0rd.contract.ContractModel;
import io.github.ih0rd.contract.SupportedLanguage;

/// # LanguageAstParser
///
/// SPI for language-specific AST parsers.
///
/// Implementations are responsible for parsing source code
/// of a specific language into a {@link ContractModel}.
///
public interface LanguageAstParser {

    /// @return supported language
    SupportedLanguage language();

    /// Parses script into contract model.
    ContractModel parse(ScriptDescriptor script, CodegenConfig config);
}
