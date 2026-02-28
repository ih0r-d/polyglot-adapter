package io.github.ih0rd.contract.types;

import java.util.List;

public record PolyUnion(List<PolyType> variants) implements PolyType {}
