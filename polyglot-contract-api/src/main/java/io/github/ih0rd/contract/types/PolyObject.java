package io.github.ih0rd.contract.types;

import java.util.Map;

public record PolyObject(Map<String, PolyType> fields) implements PolyType {}
