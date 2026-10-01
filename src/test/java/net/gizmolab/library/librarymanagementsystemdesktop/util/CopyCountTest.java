package net.gizmolab.library.librarymanagementsystemdesktop.util;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class CopyCountTest {

    @Test
    void acceptsWholeNumbersFrom1To50() {
        assertEquals(OptionalInt.of(1), CopyCount.parse("1"));
        assertEquals(OptionalInt.of(12), CopyCount.parse(" 12 "));
        assertEquals(OptionalInt.of(50), CopyCount.parse("50"));
    }

    @Test
    void rejectsEmptyTextLettersAndOutOfRange() {
        for (String text : new String[]{null, "", "  ", "abc", "3α", "0", "51", "-2", "2.5"}) {
            assertTrue(CopyCount.parse(text).isEmpty(), "should reject: " + text);
        }
    }
}
