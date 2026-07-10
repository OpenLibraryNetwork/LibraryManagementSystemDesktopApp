package net.gizmolab.library.librarymanagementsystemdesktop.validation;

/**
 * Validates ISBN-10 and ISBN-13 formats following ISO 2108 standard.
 * <p>
 * - Ignores spaces and hyphens before validation.
 * - Accepts both ISBN-10 and ISBN-13.
 * - Validates official checksum algorithms.
 * - Rejects any other length.
 * <p>
 * This class is stateless and thread-safe.
 */
public final class ISBNValidator {

    private ISBNValidator() {
        // Utility class
    }

    /**
     * Validates an ISBN string (ISBN-10 or ISBN-13).
     *
     * @param value the ISBN to validate (may contain spaces and hyphens)
     * @return true if valid, false otherwise. Null and empty are considered invalid.
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // Remove spaces and hyphens
        String cleaned = value.replaceAll("[\\s-]", "");

        if (cleaned.length() == 10) {
            return isValidISBN10(cleaned);
        } else if (cleaned.length() == 13) {
            return isValidISBN13(cleaned);
        }

        return false; // Invalid length
    }

    /**
     * Validates ISBN-10 checksum (ISO 2108).
     * <p>
     * First 9 characters must be digits. Last character may be a digit or 'X' (value 10).
     * Checksum: sum of (digit × weight) for weights 10 down to 1, must be divisible by 11.
     */
    public static boolean isValidISBN10(String isbn) {
        if (isbn == null || isbn.length() != 10) {
            return false;
        }

        // First 9 must be digits
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(isbn.charAt(i))) {
                return false;
            }
        }

        // Last character can be digit or X/x
        char lastChar = isbn.charAt(9);
        if (!Character.isDigit(lastChar) && lastChar != 'X' && lastChar != 'x') {
            return false;
        }

        // Calculate checksum
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (isbn.charAt(i) - '0') * (10 - i);
        }

        int lastValue = (lastChar == 'X' || lastChar == 'x') ? 10 : (lastChar - '0');
        sum += lastValue;

        return sum % 11 == 0;
    }

    /**
     * Validates ISBN-13 checksum (ISO/IEC 2108:2017).
     * <p>
     * Must contain exactly 13 digits. Must start with prefix 978 or 979.
     * Checksum: alternating weights of 1 and 3, total must be divisible by 10.
     */
    public static boolean isValidISBN13(String isbn) {
        if (isbn == null || isbn.length() != 13) {
            return false;
        }

        // All 13 characters must be digits
        for (int i = 0; i < 13; i++) {
            if (!Character.isDigit(isbn.charAt(i))) {
                return false;
            }
        }

        // Must start with 978 or 979
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) {
            return false;
        }

        // Calculate checksum
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        return sum % 10 == 0;
    }

    /**
     * Formats an ISBN string with standard hyphens.
     * <p>
     * ISBN-13: XXX-X-XXX-XXXXX-X (simplified grouping)
     * ISBN-10: X-XXX-XXXXX-X (simplified grouping)
     * <p>
     * Note: Real ISBN hyphenation depends on registration group ranges.
     * This uses a common simplified pattern.
     *
     * @param value raw ISBN (digits only or with existing formatting)
     * @return formatted ISBN with hyphens, or the original value if length doesn't match
     */
    public static String format(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.replaceAll("[\\s-]", "");

        if (cleaned.length() == 13) {
            // ISBN-13: XXX-X-XXX-XXXXX-X
            return cleaned.substring(0, 3) + "-" +
                   cleaned.substring(3, 4) + "-" +
                   cleaned.substring(4, 7) + "-" +
                   cleaned.substring(7, 12) + "-" +
                   cleaned.substring(12);
        } else if (cleaned.length() == 10) {
            // ISBN-10: X-XXX-XXXXX-X
            return cleaned.substring(0, 1) + "-" +
                   cleaned.substring(1, 4) + "-" +
                   cleaned.substring(4, 9) + "-" +
                   cleaned.substring(9);
        }

        return value; // Return as-is if not standard length
    }
}
