package io.github.ih0rd.contract;

/// # ContractParam
///
/// Represents a single method parameter.
///
/// Responsibilities:
/// - Preserve parameter name
/// - Preserve resolved (or fallback) type
///
public record ContractParam(String name, String type) {

}