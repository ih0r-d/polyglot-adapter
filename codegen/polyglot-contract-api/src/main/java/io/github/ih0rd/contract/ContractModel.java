package io.github.ih0rd.contract;

import java.util.List;

/// # ContractModel
///
/// Root model representing all generated contracts derived from a single script source.
///
/// Responsibilities:
/// - Aggregate all discovered contract classes
/// - Act as a transport object between parsing and generation stages
///
/// Design notes:
/// - Language-agnostic
/// - Immutable and defensive-copy safe
/// - Does not contain rendering or runtime logic
///
public record ContractModel(
        List<ContractClass> classes
) {

    public ContractModel {
        classes = List.copyOf(classes);
    }
}