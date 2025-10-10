package io.github.ih0rd.adapter.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringCaseConverterTest {
    @Test
    void snakeToCamel_handlesCases() {
        assertNull(StringCaseConverter.snakeToCamel(null));
        assertEquals("", StringCaseConverter.snakeToCamel(""));
        assertEquals("helloWorld", StringCaseConverter.snakeToCamel("hello_world"));
    }

    @Test
    void camelToSnake_handlesCases() {
        assertNull(StringCaseConverter.camelToSnake(null));
        assertEquals("", StringCaseConverter.camelToSnake(""));
        assertEquals("my_class", StringCaseConverter.camelToSnake("MyClass"));
        assertEquals("user_i_d", StringCaseConverter.camelToSnake("UserID"));
    }
}
