package io.github.ih0rd.codegen.cli;

import io.github.ih0rd.contract.CodegenConfig;
import java.nio.file.Path;

/// # CliArguments
///
/// Structured representation of CLI input.
///
/// This record decouples raw CLI parsing from the actual code generation logic.
///
public record CliArguments(
        Path inputDir,
        Path outputDir,
        CodegenConfig config
) {

}
