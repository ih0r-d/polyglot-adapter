package io.github.ih0rd.adapter.api.context;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvalResultTest {
    @Test
    void of_infersTypeAutomatically() {
        var result = EvalResult.of("text");
        assertEquals(String.class, result.type());
        assertEquals("text", result.value());
    }

    @Test
    void of_handlesNull() {
        var result = EvalResult.of(null);
        assertEquals(Object.class, result.type());
        assertNull(result.value());
    }

    @Test
    void toString_containsTypeAndValue() {
        var res = EvalResult.of(42);
        String s = res.toString();
        assertTrue(s.contains("EvalResult"));
        assertTrue(s.contains("Integer"));
        assertTrue(s.contains("42"));
    }
}
