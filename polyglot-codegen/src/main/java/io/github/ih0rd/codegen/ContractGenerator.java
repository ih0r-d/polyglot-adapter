package io.github.ih0rd.codegen;

import io.github.ih0rd.contract.CodegenConfig;
import io.github.ih0rd.contract.ContractModel;
import io.github.ih0rd.contract.ScriptDescriptor;

/// # ContractGenerator
///
/// High-level entry point for contract generation.
///
/// Responsibilities:
/// - Accept a fully materialized {@link ScriptDescriptor}
/// - Delegate language-specific parsing and structural analysis
/// - Produce a language-agnostic {@link ContractModel}
///
/// Design notes:
/// - This abstraction MUST remain language-agnostic
/// - No script I/O is allowed at this layer
/// - No runtime execution of scripts is allowed
/// - Implementations MUST rely on static, deterministic analysis only
/// - Runtime abstractions such as ScriptSource are explicitly out of scope
///
public interface ContractGenerator {

    /// ### generate
    ///
    /// Generates a contract model from the given script descriptor.
    ///
    /// @param descriptor fully resolved script descriptor
    /// @param config     code generation configuration
    /// @return generated contract model
    ContractModel generate(ScriptDescriptor descriptor, CodegenConfig config);
}