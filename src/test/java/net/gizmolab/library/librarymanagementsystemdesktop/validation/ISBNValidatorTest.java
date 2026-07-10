package net.gizmolab.library.librarymanagementsystemdesktop.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ISBNValidator covering:
 * - Valid ISBN-10 and ISBN-13
 * - Invalid checksum
 * - Hyphenated and spaced formats
 * - Edge cases (null, empty, wrong length, invalid characters)
 * - Formatting with hyphens
 */
class ISBNValidatorTest {

    @Nested
    @DisplayName("Null and empty handling")
    class NullAndEmpty {

        @Test
        @DisplayName("null value is invalid")
        void nullIsInvalid() {
            assertFalse(ISBNValidator.isValid(null));
        }

        @Test
        @DisplayName("empty string is invalid")
        void emptyStringIsInvalid() {
            assertFalse(ISBNValidator.isValid(""));
        }

        @Test
        @DisplayName("whitespace only is invalid")
        void whitespaceOnlyIsInvalid() {
            assertFalse(ISBNValidator.isValid("   "));
        }
    }

    @Nested
    @DisplayName("Valid ISBN-10")
    class ValidISBN10 {

        @Test
        @DisplayName("0306406152 is valid")
        void standardISBN10() {
            assertTrue(ISBNValidator.isValid("0306406152"));
        }

        @Test
        @DisplayName("080442957X with X check digit is valid")
        void isbn10WithX() {
            assertTrue(ISBNValidator.isValid("080442957X"));
        }

        @Test
        @DisplayName("lowercase x check digit is valid")
        void isbn10WithLowercaseX() {
            assertTrue(ISBNValidator.isValid("080442957x"));
        }

        @Test
        @DisplayName("ISBN-10 with hyphens is valid")
        void isbn10WithHyphens() {
            assertTrue(ISBNValidator.isValid("0-306-40615-2"));
        }

        @Test
        @DisplayName("ISBN-10 with spaces is valid")
        void isbn10WithSpaces() {
            assertTrue(ISBNValidator.isValid("0 306 40615 2"));
        }
    }

    @Nested
    @DisplayName("Invalid ISBN-10")
    class InvalidISBN10 {

        @Test
        @DisplayName("0306406153 has invalid checksum")
        void invalidChecksum() {
            assertFalse(ISBNValidator.isValid("0306406153"));
        }

        @Test
        @DisplayName("ISBN-10 with non-digit characters is invalid")
        void invalidCharacters() {
            assertFalse(ISBNValidator.isValid("ABC1234567"));
        }

        @Test
        @DisplayName("X in middle position is invalid")
        void xInMiddle() {
            assertFalse(ISBNValidator.isValid("03064X6152"));
        }
    }

    @Nested
    @DisplayName("Valid ISBN-13")
    class ValidISBN13 {

        @Test
        @DisplayName("9780306406157 is valid")
        void standardISBN13() {
            assertTrue(ISBNValidator.isValid("9780306406157"));
        }

        @Test
        @DisplayName("ISBN-13 with hyphens is valid")
        void isbn13WithHyphens() {
            assertTrue(ISBNValidator.isValid("978-0-306-40615-7"));
        }

        @Test
        @DisplayName("ISBN-13 with spaces is valid")
        void isbn13WithSpaces() {
            assertTrue(ISBNValidator.isValid("978 0 306 40615 7"));
        }

        @Test
        @DisplayName("979 prefix is valid")
        void isbn13With979Prefix() {
            assertTrue(ISBNValidator.isValid("9791032305690"));
        }
    }

    @Nested
    @DisplayName("Invalid ISBN-13")
    class InvalidISBN13 {

        @Test
        @DisplayName("9781234567890 has invalid checksum")
        void invalidChecksum() {
            assertFalse(ISBNValidator.isValid("9781234567890"));
        }

        @Test
        @DisplayName("ISBN-13 with non-978/979 prefix is invalid")
        void invalidPrefix() {
            assertFalse(ISBNValidator.isValid("9771234567890"));
        }

        @Test
        @DisplayName("ISBN-13 with letter is invalid")
        void letterInIsbn13() {
            assertFalse(ISBNValidator.isValid("978030640615X"));
        }
    }

    @Nested
    @DisplayName("Incorrect lengths")
    class IncorrectLengths {

        @Test
        @DisplayName("9 digits is too short")
        void tooShort() {
            assertFalse(ISBNValidator.isValid("123456789"));
        }

        @Test
        @DisplayName("12 digits is invalid length")
        void twelveDigits() {
            assertFalse(ISBNValidator.isValid("123456789012"));
        }

        @Test
        @DisplayName("14 digits is too long")
        void tooLong() {
            assertFalse(ISBNValidator.isValid("12345678901234"));
        }

        @Test
        @DisplayName("11 digits is invalid length")
        void elevenDigits() {
            assertFalse(ISBNValidator.isValid("12345678901"));
        }
    }

    @Nested
    @DisplayName("Formatting")
    class Formatting {

        @Test
        @DisplayName("Format ISBN-13 with hyphens")
        void formatIsbn13() {
            assertEquals("978-0-306-40615-7", ISBNValidator.format("9780306406157"));
        }

        @Test
        @DisplayName("Format ISBN-10 with hyphens")
        void formatIsbn10() {
            assertEquals("0-306-40615-2", ISBNValidator.format("0306406152"));
        }

        @Test
        @DisplayName("Format already-formatted ISBN-13")
        void formatAlreadyFormatted() {
            assertEquals("978-0-306-40615-7", ISBNValidator.format("978-0-306-40615-7"));
        }

        @Test
        @DisplayName("Format null returns null")
        void formatNull() {
            assertNull(ISBNValidator.format(null));
        }

        @Test
        @DisplayName("Format invalid length returns as-is")
        void formatInvalidLength() {
            assertEquals("12345", ISBNValidator.format("12345"));
        }
    }
}
