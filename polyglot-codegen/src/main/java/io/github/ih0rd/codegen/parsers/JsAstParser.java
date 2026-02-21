package io.github.ih0rd.codegen.parsers;

import io.github.ih0rd.contract.CodegenConfig;
import io.github.ih0rd.contract.ScriptDescriptor;
import io.github.ih0rd.contract.ContractModel;
import io.github.ih0rd.contract.SupportedLanguage;
import io.github.ih0rd.contract.ScriptSource;
import io.github.ih0rd.contract.LanguageAstParser;

/// # JsAstParser
///
/// JavaScript contract parser (stub).
///
/// Responsibilities:
/// - Acts as a placeholder for future JavaScript AST-based code generation
/// - Defines an explicit extension point for JS support
///
/// Design notes:
/// - JavaScript code generation is NOT supported in the current version
/// - This class intentionally contains no parsing logic
/// - No assumptions are made about JavaScript module systems or exports
/// - ScriptSource is accepted for API symmetry only
///
public final class JsAstParser implements LanguageAstParser {

    /// ### parse
    ///
    /// Attempts to parse a JavaScript script source and produce a contract model.
    ///
    /// Current behavior:
    /// - Always fails with {@link UnsupportedOperationException}
    ///
    /// @param source script source abstraction
    /// @param config code generation configuration
    /// @return never returns normally
    public ContractModel parse(ScriptSource source, CodegenConfig config) {
        throw new UnsupportedOperationException(
                "JavaScript contract generation is not supported yet");
    }

    /// ### supports
    ///
    /// Indicates whether this parser supports the given language.
    ///
    /// @param language script language
    /// @return {@code true} only for JavaScript
    public boolean supports(SupportedLanguage language) {
        return language == SupportedLanguage.JS;
    }

    @Override
    public SupportedLanguage language() {
        return null;
    }

    @Override
    public ContractModel parse(ScriptDescriptor script, CodegenConfig config) {
        return null;
    }
}