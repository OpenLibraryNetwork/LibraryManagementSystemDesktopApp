package net.gizmolab.library.librarymanagementsystemdesktop.validation;

/**
 * Validates ISSN (International Standard Serial Number) formats following ISO 3297 standard.
 * <p>
 * - Ignores spaces and hyphens before validation.
 * - Accepts 8-character ISSN (with or without hyphen).
 * - Validates checksum using modulo 11 algorithm.
 * <p>
 * This class is stateless and thread-safe.
 */
public final class ISSNValidator {

    private ISSNValidator() {
        // Utility class
    }

    /**
     * Validates an ISSN string.
     *
     * @param value the ISSN to validate (may contain spaces and hyphens)
     * @return true if valid, false otherwise. Null and empty are considered invalid.
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // Remove spaces and hyphens
        String cleaned = value.replaceAll("[\\s-]", "");

        if (cleaned.length() != 8) {
            return false;
        }

        // First 7 must be digits
        for (int i = 0; i < 7; i++) {
            if (!Character.isDigit(cleaned.charAt(i))) {
                return false;
            }
        }

        // Last character can be digit or X/x
        char lastChar = cleaned.charAt(7);
        if (!Character.isDigit(lastChar) && lastChar != 'X' && lastChar != 'x') {
            return false;
        }

        // Calculate checksum
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += (cleaned.charAt(i) - '0') * (8 - i);
        }

        int lastValue = (lastChar == 'X' || lastChar == 'x') ? 10 : (lastChar - '0');
        sum += lastValue;

        return sum % 11 == 0;
    }

    /**
     * Formats an ISSN string with a standard hyphen (XXXX-XXXX).
     *
     * @param value raw ISSN
     * @return formatted ISSN with hyphen, or original value if length doesn't match
     */
    public static String format(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.replaceAll("[\\s-]", "");

        if (cleaned.length() == 8) {
            return cleaned.substring(0, 4) + "-" + cleaned.substring(4);
        }

        return value; // Return as-is if not standard length
    }
}
