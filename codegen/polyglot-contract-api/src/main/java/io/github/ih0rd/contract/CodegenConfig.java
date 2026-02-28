package io.github.ih0rd.contract;

/// # CodegenConfig
///
/// Immutable configuration object for contract code generation.
///
/// Responsibilities:
/// - Define feature switches affecting code generation behavior
/// - Act as a pure data holder (no logic)
///
/// Design notes:
/// - This class MUST NOT depend on Spring or any configuration framework
/// - Configuration is expected to be provided by integration layers (e.g. Spring adapter, CLI, Maven plugin)
///
/// @param onlyIncludedMethods ### onlyIncludedMethods
///
///                                                       When enabled, only methods explicitly marked for inclusion
///                            (e.g. via adapter-level markers) will be generated.
public record CodegenConfig(boolean onlyIncludedMethods) {

}