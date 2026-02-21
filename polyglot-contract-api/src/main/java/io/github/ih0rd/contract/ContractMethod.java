package io.github.ih0rd.contract;

import java.util.List;

/// # ContractMethod
///
/// Represents a single method in a generated contract.
///
/// Responsibilities:
/// - Hold method name
/// - Hold ordered method parameters
///
public record ContractMethod(String name, List<ContractParam> params, String returnType) {

}