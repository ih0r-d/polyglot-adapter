package io.github.ih0rd.adapter.api.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

class EvalResultTest {

  @Test
  void of_infersTypeFromValue() {
    var result = EvalResult.of("hello");
    assertEquals(String.class, result.type());
    assertEquals("hello", result.value());
  }

  @Test
  void of_handlesNullValue() {
    var result = EvalResult.of(null);
    assertEquals(Object.class, result.type());
    assertNull(result.value());
  }

  @Test
  void as_convertsSimpleJavaTypes() {
    var result = EvalResult.of(42);
    assertEquals(42, result.as(Integer.class));

    var resultDouble = EvalResult.of(3.14);
    assertEquals(3.14, resultDouble.as(Double.class));
  }

  @Test
  void as_handlesPolyglotValueConversion() {
    try (var ctx = Context.create("js")) {
      Value jsValue = ctx.eval("js", "({a:1,b:2})");
      var result = EvalResult.of(jsValue);

      var map = result.value().as(Map.class);
      assertNotNull(map);
      assertEquals(2, map.size());
      assertEquals(1, (int) map.get("a"));
    }
  }

  @Test
  void as_handlesPrimitivePolyglotValues() {
    try (var ctx = Context.create("js")) {
      Value num = ctx.eval("js", "42");
      Value str = ctx.eval("js", "'abc'");
      Value bool = ctx.eval("js", "true");

      assertEquals(42, EvalResult.of(num).as(Integer.class));
      assertEquals("abc", EvalResult.of(str).as(String.class));
      assertEquals(true, EvalResult.of(bool).as(Boolean.class));
    }
  }

  @Test
  void as_throwsOnIncompatibleType() {
    var result = EvalResult.of("text");
    assertThrows(ClassCastException.class, () -> result.as(Integer.class));
  }

  @Test
  void toString_includesTypeAndValue() {
    var result = EvalResult.of(123);
    String out = result.toString();
    assertTrue(out.contains("EvalResult"));
    assertTrue(out.contains("Integer"));
    assertTrue(out.contains("123"));
  }

  @Test
  void toString_handlesNullType() {
    var result = new EvalResult<>(null, "x");
    String out = result.toString();
    assertTrue(out.contains("null"));
    assertTrue(out.contains("x"));
  }
}
