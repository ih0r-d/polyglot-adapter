package io.github.ih0rd.adapter.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommonUtilsTest {

  interface Dummy {
    int add(int a, int b);
  }

  static class DummyImpl implements Dummy {
    public int add(int a, int b) {
      return a + b;
    }
  }

  @Test
  void invokeMethod_invokesSuccessfully() {
    DummyImpl impl = new DummyImpl();
    EvalResult<?> result = CommonUtils.invokeMethod(Dummy.class, impl, "add", 2, 3);
    assertEquals(5, result.value());
  }

  @Test
  void invokeMethod_throwsIfNoSuchMethod() {
    assertThrows(
        EvaluationException.class,
        () -> CommonUtils.invokeMethod(Dummy.class, new DummyImpl(), "missing"));
  }

  @Test
  void getFirstElement_returnsFirstOrNull() {
    assertNull(CommonUtils.getFirstElement(Set.of()));
  }

  @Test
  void checkIfMethodExists_detectsCorrectly() {
    assertTrue(CommonUtils.checkIfMethodExists(Dummy.class, "add"));
    assertFalse(CommonUtils.checkIfMethodExists(Dummy.class, "nope"));
    assertThrows(
        EvaluationException.class, () -> CommonUtils.checkIfMethodExists(CommonUtils.class, "any"));
  }
}
