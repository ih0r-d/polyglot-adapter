package io.github.ih0rd.adapter.api.context;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LanguageTest {
    @Test
    void id_returnsPython() {
        assertEquals("python", Language.PYTHON.id());
    }

    @Test
    void id_returnsJs() {
        assertEquals("js", Language.JS.id());
    }
}
