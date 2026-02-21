package io.github.ih0rd.codegen.parsers;

import io.github.ih0rd.contract.CodegenConfig;
import io.github.ih0rd.contract.ScriptDescriptor;

import io.github.ih0rd.contract.ContractClass;
import io.github.ih0rd.contract.ContractMethod;
import io.github.ih0rd.contract.ContractModel;
import io.github.ih0rd.contract.ContractParam;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// # PythonAstParser
///
/// Structural Python AST parser (v1).
///
/// This parser performs **static, non-executing analysis** of Python source code
/// to extract contract definitions for Java interface generation.
///
/// ---
///
/// ## Supported features:
/// - Detection of exported classes via `polyglot.export_value`
/// - Extraction of top-level class methods
/// - Optional filtering via `@adapter_include` decorator
/// - Indentation-aware class body detection (tabs and spaces supported)
///
/// ## Explicit non-goals (v1):
/// - Type inference
/// - Nested classes
/// - Decorators with arguments
/// - Multiple exported classes per file
/// - Python runtime execution
///
/// ---
///
/// ## Design notes:
/// - This is **NOT** a full Python parser
/// - Indentation is treated structurally, not semantically
/// - The implementation is intentionally conservative and predictable
///
public final class PythonAstParser {

    /// Matches:
    /// polyglot.export_value("ClassName", ClassName)
    private static final Pattern EXPORT_PATTERN =
            Pattern.compile(
                    "polyglot\\.export_value\\(\\s*[\"'](\\w+)[\"']\\s*,\\s*(\\w+)\\s*\\)");

    /// Matches:
    /// class ClassName:
    private static final Pattern CLASS_PATTERN =
            Pattern.compile("^\\s*class\\s+(\\w+)\\s*:");

    /// Matches:
    /// def method(self, ...):
    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^\\s*def\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*:");


    /// Marker decorator used to explicitly include methods
    private static final String PYTHON_DECORATOR_INCLUDE = "@adapter_include";

    /// Normalized indentation width for a single tab
    private static final int TAB_WIDTH = 4;

    /// ### parse
    ///
    /// Parses Python source code and produces a {@link ContractModel}.
    ///
    /// @param script script descriptor containing source and metadata
    /// @param config code generation configuration
    /// @return generated contract model
    ///
    /// @throws IllegalStateException if no exported class is found
    public ContractModel parse(ScriptDescriptor script, CodegenConfig config) {
        String source = script.source();
        String[] lines = source.split("\\R");

        String exportedClass = findExportedClass(source);
        if (exportedClass == null) {
            throw new IllegalStateException(
                    "No polyglot.export_value found"
                            + (script.fileName() != null ? " in " + script.fileName() : "")
            );
        }

        boolean insideClass = false;
        boolean includeNext = false;
        int classIndentLevel = -1;

        List<ContractMethod> methods = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();

            // --- Class detection -------------------------------------------------
            if (!insideClass) {
                Matcher classMatcher = CLASS_PATTERN.matcher(line);
                if (classMatcher.find()
                        && classMatcher.group(1).equals(exportedClass)) {
                    insideClass = true;
                    classIndentLevel = indentLevel(rawLine);
                }
                continue;
            }

            // --- Exit class on dedent -------------------------------------------
            if (!line.isBlank()
                    && indentLevel(rawLine) <= classIndentLevel) {
                break;
            }

            // --- Decorator detection --------------------------------------------
            if (line.trim().equals(PYTHON_DECORATOR_INCLUDE)) {
                includeNext = true;
                continue;
            }

            // --- Method detection -----------------------------------------------
            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.find()) {
                String methodName = methodMatcher.group(1);
                String paramsRaw = methodMatcher.group(2);

                boolean allowed =
                        !methodName.startsWith("_")
                                && (!config.onlyIncludedMethods() || includeNext);

                includeNext = false;

                if (allowed) {
                    methods.add(
                            new ContractMethod(
                                    methodName,
                                    parseParams(paramsRaw),
                                    "Object"
                            )
                    );
                }
            }
        }

        return new ContractModel(
                List.of(new ContractClass(exportedClass, methods))
        );
    }

    /// ### findExportedClass
    ///
    /// Detects the exported class name from `polyglot.export_value`.
    private String findExportedClass(String source) {
        Matcher m = EXPORT_PATTERN.matcher(source);
        return m.find() ? m.group(2) : null;
    }

    /// ### parseParams
    ///
    /// Parses method parameters and drops `self`.
    ///
    /// All parameter types are mapped to `Object` in v1.
    private List<ContractParam> parseParams(String raw) {
        List<ContractParam> params = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return params;
        }

        String[] parts = raw.split(",");
        for (String p : parts) {
            String name = p.trim();
            if (name.equals("self") || name.isEmpty()) {
                continue;
            }
            params.add(new ContractParam(name, "Object"));
        }
        return params;
    }

    /// ### indentLevel
    ///
    /// Computes indentation level of a line.
    ///
    /// Rules:
    /// - Spaces count as 1
    /// - Tabs count as {@link #TAB_WIDTH}
    ///
    /// @param line raw source line
    /// @return normalized indentation level
    private int indentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += TAB_WIDTH;
            } else {
                break;
            }
        }
        return count;
    }
}
