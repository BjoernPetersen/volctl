package net.bjoernpetersen.volctl;

import org.junit.jupiter.api.Test;

import static net.bjoernpetersen.volctl.StringUtils.substringAfterLast;
import static net.bjoernpetersen.volctl.StringUtils.substringBeforeLast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StringUtilsTest {
    @Test
    void substringAfterNotContained() {
        String testString = "asdf";
        assertEquals(testString, substringAfterLast(testString, '.'));
    }

    @Test
    void substringAfterStart() {
        assertEquals("asdf", substringAfterLast(".asdf", '.'));
    }

    @Test
    void substringAfterMiddle() {
        assertEquals("ghjk", substringAfterLast("asdf.ghjk", '.'));
    }

    @Test
    void substringAfterEnd() {
        assertTrue(substringAfterLast("asdf.", '.').isEmpty());
    }

    @Test
    void substringAfterMultiple() {
        assertEquals("l", substringAfterLast("asdf.ghjk.l", '.'));
    }

    @Test
    void substringBeforeNotContained() {
        String testString = "asdf";
        assertEquals(testString, substringBeforeLast(testString, '.'));
    }

    @Test
    void substringBeforeStart() {
        assertTrue(substringBeforeLast(".asdf", '.').isEmpty());
    }

    @Test
    void substringBeforeMiddle() {
        assertEquals("asdf", substringBeforeLast("asdf.ghjk", '.'));
    }

    @Test
    void substringBeforeEnd() {
        assertEquals("asdf", substringBeforeLast("asdf.", '.'));
    }

    @Test
    void substringBeforeMultiple() {
        assertEquals("asdf.ghjk", substringBeforeLast("asdf.ghjk.l", '.'));
    }
}
