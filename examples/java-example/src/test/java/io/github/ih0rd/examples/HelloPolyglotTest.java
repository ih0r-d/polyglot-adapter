package io.github.ih0rd.examples;

import static org.junit.jupiter.api.Assertions.*;

import io.github.ih0r.adapter.api.PolyglotAdapter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HelloPolyglotTest {

    @Test
    void testAdd() {
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            Map<String, Object> result = adapter.evaluate("add", MyApi.class, 2, 5);
            assertEquals(7, result.get("result"));
        }
    }

    @Test
    void testPing() {
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            Map<String, Object> result = adapter.evaluate("ping", MyApi.class);
            assertTrue(result.containsKey("result"));
        }
    }
}
