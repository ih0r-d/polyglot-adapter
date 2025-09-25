package io.github.ih0r.adapter.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringCaseConverterTest {

  @Test
  void snakeToCamel_handlesNullAndEmpty() {
    assertNull(StringCaseConverter.snakeToCamel(null));
    assertEquals("", StringCaseConverter.snakeToCamel(""));
  }

  @Test
  void snakeToCamel_convertsProperly() {
    assertEquals("helloWorld", StringCaseConverter.snakeToCamel("hello_world"));
    assertEquals("a", StringCaseConverter.snakeToCamel("a"));
    assertEquals("myLongVariableName", StringCaseConverter.snakeToCamel("my_long_variable_name"));
  }

  @Test
  void camelToSnake_handlesNullAndEmpty() {
    assertNull(StringCaseConverter.camelToSnake(null));
    assertEquals("", StringCaseConverter.camelToSnake(""));
  }

  @Test
  void camelToSnake_convertsProperly_perCurrentImplementation() {
    // Per current implementation, consecutive capitals are split between each char
    assertEquals("my_class", StringCaseConverter.camelToSnake("MyClass"));
    assertEquals("x", StringCaseConverter.camelToSnake("X"));
    assertEquals("user_i_d", StringCaseConverter.camelToSnake("UserID"));
    assertEquals("h_t_t_p_server_error", StringCaseConverter.camelToSnake("HTTPServerError"));
  }
}
