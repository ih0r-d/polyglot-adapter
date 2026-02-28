package io.github.ih0rd.contract;

import io.github.ih0rd.contract.types.PolyType;

/// # ContractParam
///
/// Represents a single method parameter.
///
/// Responsibilities:
/// - Preserve parameter name
/// - Preserve canonical, language-agnostic type
///
/// Design notes:
/// - Uses {@link PolyType} instead of raw String types
/// - No Java- or Python-specific concepts allowed here
/// - Immutable by design
///
public record ContractParam(
        String name,
        PolyType type
) {
}