package io.github.ih0rd.contract;

import io.github.ih0rd.contract.types.PolyType;

import java.util.List;

/// # ContractMethod
///
/// Represents a single method in a generated contract.
///
/// Responsibilities:
/// - Hold method name
/// - Hold ordered method parameters
/// - Hold canonical return type (language-agnostic)
///
/// Design notes:
/// - Uses {@link PolyType} instead of raw String types
/// - Immutable and defensive-copy safe
/// - No Java- or Python-specific concepts allowed here
///
public record ContractMethod(
        String name,
        List<ContractParam> params,
        PolyType returnType
) {

    public ContractMethod {
        params = List.copyOf(params);
    }
}