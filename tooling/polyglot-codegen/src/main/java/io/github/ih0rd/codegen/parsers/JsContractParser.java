package io.github.ih0rd.codegen.parsers;

import io.github.ih0rd.contract.CodegenConfig;
import io.github.ih0rd.contract.LanguageParser;
import io.github.ih0rd.contract.ScriptDescriptor;
import io.github.ih0rd.contract.ContractModel;
import io.github.ih0rd.contract.SupportedLanguage;

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
public final class JsContractParser implements LanguageParser {
    @Override
    public SupportedLanguage language() {
        return SupportedLanguage.JS;
    }

    @Override
    public ContractModel parse(ScriptDescriptor scriptDescriptor, CodegenConfig codegenConfig) {
        throw new UnsupportedOperationException(
                "JavaScript contract generation is not supported yet");
    }

}