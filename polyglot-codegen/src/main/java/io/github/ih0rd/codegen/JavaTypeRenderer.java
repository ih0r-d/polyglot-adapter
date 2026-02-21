package io.github.ih0rd.codegen;

import io.github.ih0rd.contract.types.PolyList;
import io.github.ih0rd.contract.types.PolyMap;
import io.github.ih0rd.contract.types.PolyPrimitive;
import io.github.ih0rd.contract.types.PolyType;
import java.util.HashSet;
import java.util.Set;

public final class JavaTypeRenderer {

    private final Set<String> imports = new HashSet<>();

    public String render(PolyType type) {

        if (type instanceof PolyPrimitive p) {
            return switch (p) {
                case INT -> "Integer";
                case FLOAT -> "Double";
                case STRING -> "String";
                case BOOLEAN -> "Boolean";
            };
        }

        if (type instanceof PolyList(PolyType elementType)) {
            imports.add("java.util.List");
            return "List<" + render(elementType) + ">";
        }

        if (type instanceof PolyMap(PolyType keyType, PolyType valueType)) {
            imports.add("java.util.Map");
            return "Map<" +
                    render(keyType) + ", " +
                    render(valueType) +
                    ">";
        }

        return "Object";
    }

    public Set<String> getImports() {
        return imports;
    }
}