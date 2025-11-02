package io.github.ih0rd.adapter.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

class ValueUnwrapperTest {

  @Test
  void unwrap_returnsSimpleTypes() {
    try (var ctx = Context.create("js")) {
      Value v1 = ctx.eval("js", "42");
      assertTrue(v1.isNumber());

      assertEquals(42.0, ValueUnwrapper.unwrap(v1));

      Value v2 = ctx.eval("js", "'hi'");
      assertEquals("hi", ValueUnwrapper.unwrap(v2));

      Value v3 = ctx.eval("js", "true");
      assertEquals(true, ValueUnwrapper.unwrap(v3));
    }
  }

  @Test
  void unwrap_handlesArraysAndObjects() {
    try (var ctx = Context.create("js")) {
      Value arr = ctx.eval("js", "[1,2,3]");
      List<?> list = ValueUnwrapper.unwrap(arr);
      assertEquals(3, list.size());

      Value obj = ctx.eval("js", "({x:1,y:2})");
      Map<?, ?> map = ValueUnwrapper.unwrap(obj);
      assertEquals(1.0, map.get("x"));
    }
  }

  @Test
  void unwrap_typedConversionWorks() {
    try (var ctx = Context.create("js")) {
      Value val = ctx.eval("js", "3.14");
      Double result = ValueUnwrapper.unwrap(val, Double.class);
      assertEquals(3.14, result);
    }
  }

  @Test
  void unwrap_handlesNull() {
    assertNull(ValueUnwrapper.unwrap((Value) null));
  }
}
