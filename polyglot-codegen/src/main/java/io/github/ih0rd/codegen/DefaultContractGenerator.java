package io.github.ih0rd.codegen;

import io.github.ih0rd.contract.*;
import io.github.ih0rd.codegen.parsers.*;

import java.util.EnumMap;
import java.util.Map;

/// # DefaultContractGenerator
///
/// Default implementation of `ContractGenerator`.
///
/// Coordinates language-specific `LanguageParser` implementations
/// and produces a language-agnostic `ContractModel`.
///
/// ---
///
/// ## Responsibilities
///
/// - Inspect `ScriptDescriptor` language
/// - Resolve appropriate `LanguageParser`
/// - Delegate static analysis to parser
/// - Return structural `ContractModel`
///
/// ---
///
/// ## Design Principles
///
/// - Fully language-agnostic orchestration layer
/// - No script I/O
/// - No runtime execution
/// - Deterministic static analysis only
/// - Parsers are pluggable via registry
///
/// ---
///
/// ## Architecture
///
/// ```text
/// ScriptDescriptor
///        ↓
/// DefaultContractGenerator
///        ↓
/// LanguageParser (per language)
///        ↓
/// ContractModel
/// ```
///
/// ---
///
/// New languages are added by registering a new `LanguageParser`
/// implementation without modifying dispatch logic.
///
public final class DefaultContractGenerator implements ContractGenerator {

    private final Map<SupportedLanguage, LanguageParser> parsers =
            new EnumMap<>(SupportedLanguage.class);

    public DefaultContractGenerator() {
        register(new PythonContractParser());
        register(new JsContractParser());
    }

    private void register(LanguageParser parser) {
        parsers.put(parser.language(), parser);
    }

    @Override
    public ContractModel generate(
            ScriptDescriptor descriptor,
            CodegenConfig config
    ) {
        LanguageParser parser = parsers.get(descriptor.language());

        if (parser == null) {
            throw new IllegalStateException(
                    "No parser registered for language: " + descriptor.language()
            );
        }

        return parser.parse(descriptor, config);
    }
}