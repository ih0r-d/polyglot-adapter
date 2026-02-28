package io.github.ih0rd.contract.types;

sealed public interface PolyType
        permits PolyPrimitive,
        PolyList,
        PolyMap,
        PolyObject,
        PolyUnion,
        PolyUnknown {}