package io.github.ih0r.adapter.utils;

import static io.github.ih0r.adapter.utils.Constants.PROJ_PY_RESOURCES_PATH;
import static org.junit.jupiter.api.Assertions.*;

import io.github.ih0r.adapter.exceptions.EvaluationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CommonUtilsTest {

  interface MyOps {
    int add(int a, int b);

    void ping();
  }

  static class MyOpsImpl implements MyOps {
    @Override
    public int add(int a, int b) {
      return a + b;
    }

    @Override
    public void ping() {
      /* no-op */
    }
  }

  private static Path createdFile;

  @BeforeAll
  static void setupResourcesDir() throws IOException {
    Path createdDir = Path.of(PROJ_PY_RESOURCES_PATH);
    if (!Files.exists(createdDir)) {
      Files.createDirectories(createdDir);
    }
    createdFile = createdDir.resolve("my_ops.py");
    Files.writeString(createdFile, "# dummy python file for tests\n");
  }

  @AfterAll
  static void cleanupResourcesDir() throws IOException {
    // Keep directory to not affect possible other tests, but remove our file if exists
    if (createdFile != null && Files.exists(createdFile)) {
      Files.delete(createdFile);
    }
  }

  @Test
  void invokeMethod_returnsResultAndType() {
    MyOps target = new MyOpsImpl();
    Map<String, Object> res = CommonUtils.invokeMethod(MyOps.class, target, "add", 2, 3);
    assertEquals("int", res.get("returnType"));
    assertEquals(5, res.get("result"));
  }

  @Test
  void invokeMethod_voidReturnsOptionalEmpty() {
    MyOps target = new MyOpsImpl();
    Map<String, Object> res = CommonUtils.invokeMethod(MyOps.class, target, "ping");
    assertEquals("void", res.get("returnType"));
    assertInstanceOf(Optional.class, res.get("result"));
    assertTrue(((Optional<?>) res.get("result")).isEmpty());
  }

  @Test
  void invokeMethod_throwsOnUnknownMethod() {
    MyOps target = new MyOpsImpl();
    EvaluationException ex =
        assertThrows(
            EvaluationException.class,
            () -> CommonUtils.invokeMethod(MyOps.class, target, "unknown", 1));
    assertTrue(ex.getMessage().contains("unknown"));
  }

  @Test
  void checkIfMethodExists_positiveAndNegative() {
    assertTrue(CommonUtils.checkIfMethodExists(MyOps.class, "add"));
    assertFalse(CommonUtils.checkIfMethodExists(MyOps.class, "subtract"));
  }

  static class NotAnInterface {}

  @Test
  void checkIfMethodExists_throwsWhenNotInterface() {
    EvaluationException ex =
        assertThrows(
            EvaluationException.class,
            () -> CommonUtils.checkIfMethodExists(NotAnInterface.class, "anything"));
    assertTrue(ex.getMessage().contains("must be an interface"));
  }

  @Test
  void getFirstElement_nullOrEmpty() {
    assertNull(CommonUtils.getFirstElement(null));
    assertNull(CommonUtils.getFirstElement(Set.of()));
  }

  @Test
  void getFirstElement_returnsFirstFromOrderedSet() {
    LinkedHashSet<String> set = new LinkedHashSet<>();
    set.add("first");
    set.add("second");
    assertEquals("first", CommonUtils.getFirstElement(set));
  }

  @Test
  void checkFileExists_findsCreatedFile() {
    assertTrue(CommonUtils.checkFileExists("My_Ops").isPresent());
    assertTrue(CommonUtils.checkFileExists("my_ops").isPresent());
    assertTrue(CommonUtils.checkFileExists("MY_OPS").isPresent());
  }

  @Test
  void checkFileExists_returnsEmptyForUnknown() {
    assertTrue(CommonUtils.checkFileExists("nonexistent_123_xyz").isEmpty());
  }
}
