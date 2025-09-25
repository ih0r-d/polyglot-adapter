package io.github.ih0r.adapter.api.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageTest {

    @Test
    void id_returnsPython() {
        assertEquals("python", Language.PYTHON.id());
    }
}
