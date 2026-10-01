package net.gizmolab.library.librarymanagementsystemdesktop.util;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ISSN and the EAN-13 barcode of serials: 977 + first 7 ISSN digits + 2 variant digits + check digit,
 * optionally followed by a 2- or 5-digit add-on. Mirrors library-strapi/src/utils/issn.js.
 */
public final class Issn {

    private static final Pattern ISSN_SHAPE = Pattern.compile("\\d{7}[\\dX]");
    private static final Pattern BARCODE_SHAPE = Pattern.compile("\\d{13}(\\d{2}|\\d{5})?");
    private static final Pattern SERIAL_EAN = Pattern.compile("977\\d{10}(\\d{2}|\\d{5})?");

    private Issn() {}

    private static String compact(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
    }

    /** ISSN- or barcode-shaped input (as opposed to a title), valid or not. */
    public static boolean looksLikeCode(String raw) {
        String c = compact(raw);
        return ISSN_SHAPE.matcher(c).matches() || BARCODE_SHAPE.matcher(c).matches();
    }

    public static Optional<String> normalize(String raw) {
        String c = compact(raw);
        if (!ISSN_SHAPE.matcher(c).matches() || issnCheckDigit(c.substring(0, 7)) != c.charAt(7)) return Optional.empty();
        return Optional.of(c.substring(0, 4) + "-" + c.substring(4));
    }

    /** ISSN or serial barcode → ISSN. */
    public static Optional<String> parseCode(String raw) {
        String c = compact(raw);
        if (!BARCODE_SHAPE.matcher(c).matches()) return normalize(raw);
        if (!SERIAL_EAN.matcher(c).matches() || eanCheckDigit(c.substring(0, 12)) != c.charAt(12)) return Optional.empty();
        String seven = c.substring(3, 10);
        return Optional.of(seven.substring(0, 4) + "-" + seven.substring(4) + issnCheckDigit(seven));
    }

    private static char issnCheckDigit(String seven) {
        int sum = 0;
        for (int i = 0; i < 7; i++) sum += (seven.charAt(i) - '0') * (8 - i);
        int r = (11 - sum % 11) % 11;
        return r == 10 ? 'X' : (char) ('0' + r);
    }

    private static char eanCheckDigit(String twelve) {
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (twelve.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return (char) ('0' + (10 - sum % 10) % 10);
    }
}
