package io.github.ih0rd.codegen.parsers;

import io.github.ih0rd.codegen.types.PythonTypeMapper;
import io.github.ih0rd.contract.*;
import io.github.ih0rd.contract.types.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PythonAstParser {

    private static final Pattern EXPORT_PATTERN =
            Pattern.compile("polyglot\\.export_value\\(\\s*[\"'](\\w+)[\"']\\s*,\\s*(\\w+)\\s*\\)");

    private static final Pattern CLASS_PATTERN =
            Pattern.compile("^\\s*class\\s+(\\w+)\\s*:");

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("^\\s*def\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*(?:->\\s*(\\w+))?\\s*:");

    private static final Pattern PARAM_PATTERN =
            Pattern.compile("(\\w+)\\s*(?::\\s*(\\w+))?");

    private final PythonTypeMapper mapper = new PythonTypeMapper();

    public ContractModel parse(ScriptDescriptor script, CodegenConfig config) {

        String source = script.source();
        String[] lines = source.split("\\R");

        String exportedClass = findExportedClass(source);
        if (exportedClass == null) {
            throw new IllegalStateException("No polyglot.export_value found");
        }

        boolean insideClass = false;
        int classIndent = -1;

        List<ContractMethod> methods = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {

            String rawLine = lines[i];
            String line = rawLine.stripTrailing();

            if (!insideClass) {
                Matcher classMatcher = CLASS_PATTERN.matcher(line);
                if (classMatcher.find()
                        && classMatcher.group(1).equals(exportedClass)) {
                    insideClass = true;
                    classIndent = indentLevel(rawLine);
                }
                continue;
            }

            if (!line.isBlank() && indentLevel(rawLine) <= classIndent) {
                break;
            }

            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (!methodMatcher.find()) {
                continue;
            }

            String methodName = methodMatcher.group(1);
            if (methodName.startsWith("_")) {
                continue;
            }

            String rawParams = methodMatcher.group(2);
            String rawReturn = methodMatcher.group(3);

            List<ContractParam> params = parseParams(rawParams);

            PolyType returnType;

            if (rawReturn != null) {
                returnType = mapper.mapPrimitive(rawReturn);
            } else {
                returnType = inferReturnType(lines, i + 1, indentLevel(rawLine));
            }

            methods.add(
                    new ContractMethod(methodName, params, returnType)
            );
        }

        return new ContractModel(
                List.of(new ContractClass(exportedClass, methods))
        );
    }

    private PolyType inferReturnType(String[] lines, int start, int methodIndent) {

        for (int i = start; i < lines.length; i++) {

            String line = lines[i].trim();

            if (line.startsWith("return [")) {
                return new PolyList(PolyPrimitive.INT); // simple heuristic
            }

            if (line.startsWith("return {")) {
                return new PolyMap(PolyPrimitive.STRING, PolyPrimitive.INT);
            }

            if (!line.isBlank() && indentLevel(lines[i]) <= methodIndent) {
                break;
            }
        }

        return new PolyUnknown();
    }

    private String findExportedClass(String source) {
        Matcher m = EXPORT_PATTERN.matcher(source);
        return m.find() ? m.group(2) : null;
    }

    private List<ContractParam> parseParams(String raw) {

        List<ContractParam> params = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return params;
        }

        String[] parts = raw.split(",");

        for (String part : parts) {

            String trimmed = part.trim();
            if (trimmed.equals("self")) continue;

            Matcher matcher = PARAM_PATTERN.matcher(trimmed);
            if (!matcher.find()) continue;

            String name = matcher.group(1);
            String rawType = matcher.group(2);

            PolyType type =
                    rawType != null
                            ? mapper.mapPrimitive(rawType)
                            : new PolyUnknown();

            params.add(new ContractParam(name, type));
        }

        return params;
    }

    private int indentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else if (c == '\t') count += 4;
            else break;
        }
        return count;
    }
}