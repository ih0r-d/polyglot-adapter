package io.github.ih0rd.adapter.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringCaseConverterTest {

    @Test
    void camelToSnake_convertsCorrectly() {
        assertEquals("my_class_name", StringCaseConverter.camelToSnake("MyClassName"));
    }

    @Test
    void camelToSnake_handlesNullAndEmpty() {
        assertNull(StringCaseConverter.camelToSnake(null));
        assertEquals("", StringCaseConverter.camelToSnake(""));
    }

    @Test
    void snakeToCamel_convertsCorrectly() {
        assertEquals("myClassName", StringCaseConverter.snakeToCamel("my_class_name"));
    }

    @Test
    void snakeToCamel_handlesNullAndEmpty() {
        assertNull(StringCaseConverter.snakeToCamel(null));
        assertEquals("", StringCaseConverter.snakeToCamel(""));
    }
}
