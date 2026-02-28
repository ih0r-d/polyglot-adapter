package io.github.ih0rd.codegen.cli;

import io.github.ih0rd.contract.CodegenConfig;

import java.nio.file.Path;

/// # CliArguments
///
/// Structured representation of CLI input.
///
/// Decouples raw CLI parsing from code generation logic.
public record CliArguments(
        Path inputDir,
        Path outputDir,
        String basePackage,
        CodegenConfig config
) {
}