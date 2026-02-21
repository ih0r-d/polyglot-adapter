package io.github.ih0rd.contract;

import java.util.List;

/// # ContractClass
///
/// Represents a single generated contract class.
///
/// Responsibilities:
/// - Hold the contract name
/// - Aggregate contract methods
///
/// Design notes:
/// - This model is language-agnostic
/// - No Java- or Python-specific concepts are allowed here
/// - Immutable and safe by design
///
public record ContractClass(
        String name,
        List<ContractMethod> methods
) {

    public ContractClass {
        methods = List.copyOf(methods);
    }
}