package io.github.ih0rd.codegen.types;

import io.github.ih0rd.contract.types.PolyPrimitive;
import io.github.ih0rd.contract.types.PolyType;
import io.github.ih0rd.contract.types.PolyUnknown;

import java.util.Map;

/// # PythonTypeMapper
///
/// Maps Python type annotations to canonical {@link PolyType}.
///
/// Responsibilities:
/// - Convert Python primitive type names to canonical type model
/// - Provide safe fallback for unsupported or unknown types
///
/// Design notes:
/// - This class handles ONLY primitive mappings (v1)
/// - Container types (list, dict) are handled separately
/// - No Java-specific logic is allowed here
/// - No rendering concerns — only canonical type mapping
///
public final class PythonTypeMapper implements LanguageTypeMapper {

    private static final Map<String, PolyType> PRIMITIVES = Map.of(
            "int", PolyPrimitive.INT,
            "float", PolyPrimitive.FLOAT,
            "str", PolyPrimitive.STRING,
            "bool", PolyPrimitive.BOOLEAN
    );

    /// ### mapPrimitive
    ///
    /// Maps a Python primitive type name to {@link PolyType}.
    ///
    /// @param languageType Python type name (e.g. "int", "str")
    /// @return canonical {@link PolyType}, or {@link PolyUnknown} if unsupported
    ///
    @Override
    public PolyType mapPrimitive(String languageType) {
        if (languageType == null || languageType.isBlank()) {
            return new PolyUnknown();
        }

        return PRIMITIVES.getOrDefault(
                languageType.trim(),
                new PolyUnknown()
        );
    }
}