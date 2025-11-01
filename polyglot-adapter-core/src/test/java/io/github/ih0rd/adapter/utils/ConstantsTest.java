package io.github.ih0rd.adapter.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void constants_haveExpectedValues() {
        assertTrue(Constants.DEFAULT_PY_RESOURCES.endsWith("/src/main/python"));
        assertTrue(Constants.DEFAULT_JS_RESOURCES.endsWith("/src/main/js"));
        assertNotNull(Constants.USER_DIR);
    }
}
