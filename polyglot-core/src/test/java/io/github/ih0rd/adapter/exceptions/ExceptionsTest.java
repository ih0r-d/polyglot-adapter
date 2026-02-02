package io.github.ih0rd.adapter.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ExceptionsTest {

  @Test
  void testBindingException() {
    BindingException e1 = new BindingException("msg");
    assertEquals("msg", e1.getMessage());

    BindingException e2 = new BindingException("msg", new RuntimeException());
    assertEquals("msg", e2.getMessage());
    assertNotNull(e2.getCause());
  }

  @Test
  void testEvaluationException() {
    EvaluationException e1 = new EvaluationException("msg");
    assertEquals("msg", e1.getMessage());

    EvaluationException e2 = new EvaluationException("msg", new RuntimeException());
    assertEquals("msg", e2.getMessage());
    assertNotNull(e2.getCause());
  }

  @Test
  void testInvocationException() {
    InvocationException e1 = new InvocationException("msg");
    assertEquals("msg", e1.getMessage());

    InvocationException e2 = new InvocationException("msg", new RuntimeException());
    assertEquals("msg", e2.getMessage());
    assertNotNull(e2.getCause());
  }

  @Test
  void testScriptNotFoundException() {
    ScriptNotFoundException e1 = new ScriptNotFoundException("msg");
    assertEquals("msg", e1.getMessage());

    ScriptNotFoundException e2 = new ScriptNotFoundException("msg", new RuntimeException());
    assertEquals("msg", e2.getMessage());
    assertNotNull(e2.getCause());
  }
}
