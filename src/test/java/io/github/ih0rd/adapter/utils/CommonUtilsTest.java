package io.github.ih0rd.adapter.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.ih0rd.adapter.exceptions.EvaluationException;

class CommonUtilsTest {

  interface MathApi {
    int add(int a, int b);

    String greet(String name);
  }

  static class MathImpl implements MathApi {
    @Override
    public int add(int a, int b) {
      return a + b;
    }

    @Override
    public String greet(String name) {
      return "Hello, " + name;
    }
  }

  @Test
  void invokeMethod_callsTargetMethod_withoutArgs() {
    var impl = new MathImpl();
    var res = CommonUtils.invokeMethod(MathApi.class, impl, "greet", "World");
    assertEquals("Hello, World", res.value());
  }

  @Test
  void invokeMethod_coercesWrapperNumbers_forPrimitiveParams() {
    var impl = new MathImpl();
    var res =
        CommonUtils.invokeMethod(MathApi.class, impl, "add", Integer.valueOf(2), Long.valueOf(3));
    assertEquals(5, res.value());
  }

  @Test
  void invokeMethod_throwsEvaluationException_onError() {
    var impl = new MathImpl();
    var ex =
        assertThrows(
            EvaluationException.class,
            () -> CommonUtils.invokeMethod(MathApi.class, impl, "missing"));
    assertTrue(ex.getMessage().contains("missing"));
  }

  @Test
  void checkIfMethodExists_validatesInterfaceAndPresence() {
    assertTrue(CommonUtils.checkIfMethodExists(MathApi.class, "add"));
    assertFalse(CommonUtils.checkIfMethodExists(MathApi.class, "sub"));
  }

  @Test
  void checkIfMethodExists_throwsIfNotInterface() {
    var ex =
        assertThrows(
            EvaluationException.class,
            () -> CommonUtils.checkIfMethodExists(MathImpl.class, "add"));
    assertTrue(ex.getMessage().contains("must be an interface"));
  }

  @Test
  void getFirstElement_handlesEmptyAndNonEmpty() {
    assertNull(CommonUtils.getFirstElement(java.util.Set.of()));
    assertEquals(
        "a",
        CommonUtils.getFirstElement(new java.util.LinkedHashSet<>(java.util.List.of("a", "b"))));
  }
}
