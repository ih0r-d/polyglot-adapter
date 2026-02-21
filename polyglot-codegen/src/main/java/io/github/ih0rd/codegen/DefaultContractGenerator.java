package io.github.ih0rd.codegen;

import io.github.ih0rd.contract.CodegenConfig;
import io.github.ih0rd.contract.ContractModel;
import io.github.ih0rd.contract.ScriptDescriptor;
import io.github.ih0rd.contract.SupportedLanguage;
import io.github.ih0rd.codegen.parsers.JsAstParser;
import io.github.ih0rd.codegen.parsers.PythonAstParser;

/// # DefaultContractGenerator
///
/// Default implementation of {@link ContractGenerator}.
///
/// Coordinates language-specific parsers and produces a
/// language-agnostic {@link ContractModel}.
///
/// ---
///
/// ## Responsibilities:
/// - Inspect {@link ScriptDescriptor} language
/// - Delegate to appropriate language parser
/// - Return structural contract model
///
/// ## Design notes:
/// - Acts as a simple dispatcher
/// - Uses explicit switch for clarity and predictability
/// - No I/O is performed at this layer
/// - No runtime execution of scripts is allowed
/// - New languages are added incrementally via new parser instances
///
public final class DefaultContractGenerator implements ContractGenerator {

    private final PythonAstParser pythonParser = new PythonAstParser();
    private final JsAstParser jsParser = new JsAstParser();

    @Override
    public ContractModel generate(
            ScriptDescriptor descriptor,
            CodegenConfig config
    ) {

        SupportedLanguage language = descriptor.language();

        return switch (language) {
            case PYTHON -> pythonParser.parse(descriptor, config);
            case JS -> jsParser.parse(descriptor, config);
        };
    }
}