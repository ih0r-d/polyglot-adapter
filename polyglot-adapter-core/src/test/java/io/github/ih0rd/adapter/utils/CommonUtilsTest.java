package io.github.ih0rd.adapter.utils;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CommonUtilsTest {

    interface MyOps {
        int add(int a, int b);
        void ping();
    }

    static class Impl implements MyOps {
        public int add(int a, int b) { return a + b; }
        public void ping() {}
    }

    @Test
    void invokeMethod_returnsResult() {
        var res = CommonUtils.invokeMethod(MyOps.class, new Impl(), "add", 2, 3);
        assertEquals(Integer.class, res.type());
        assertEquals(5, res.value());
    }

    @Test
    void invokeMethod_voidReturnsNullType() {
        var res = CommonUtils.invokeMethod(MyOps.class, new Impl(), "ping");
        assertEquals(Object.class, res.type());
        assertNull(res.value());
    }

    @Test
    void invokeMethod_throwsIfNoSuchMethod() {
        assertThrows(EvaluationException.class, () ->
                CommonUtils.invokeMethod(MyOps.class, new Impl(), "missing"));
    }

    @Test
    void checkIfMethodExists_works() {
        assertTrue(CommonUtils.checkIfMethodExists(MyOps.class, "add"));
        assertFalse(CommonUtils.checkIfMethodExists(MyOps.class, "missing"));
    }

    @Test
    void checkIfMethodExists_failsOnNonInterface() {
        assertThrows(EvaluationException.class,
                () -> CommonUtils.checkIfMethodExists(Impl.class, "add"));
    }

    @Test
    void getFirstElement_handlesAllCases() {
        assertNull(CommonUtils.getFirstElement(null));
        assertNull(CommonUtils.getFirstElement(Set.of()));
        LinkedHashSet<String> s = new LinkedHashSet<>();
        s.add("a"); s.add("b");
        assertEquals("a", CommonUtils.getFirstElement(s));
    }
}
