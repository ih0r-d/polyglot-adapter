package io.github.ih0r.adapter.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationExceptionTest {

    @Test
    void constructorWithMessage() {
        EvaluationException ex = new EvaluationException("msg");
        assertEquals("msg", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void constructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("boom");
        EvaluationException ex = new EvaluationException("msg2", cause);
        assertEquals("msg2", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
