package net.gizmolab.library.librarymanagementsystemdesktop.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

/**
 * Same normalization as the server's searchKey: no accents, lower case, final sigma → sigma,
 * punctuation → space. Used to decide whether a query is worth sending and for local filtering.
 */
public final class SearchText {

    private SearchText() {}

    public static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('ς', 'σ')
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    /** At least two letters or digits: "Ν." or "--" are not sent to the server (it would answer 400). */
    public static boolean isSearchable(String query) {
        return normalize(query).replace(" ", "").length() >= 2;
    }

    /** Every word of the query appears in the text (accent/case insensitive). An empty query matches. */
    public static boolean matches(String text, String query) {
        String haystack = normalize(text);
        String q = normalize(query);
        if (q.isEmpty()) return true;
        return Arrays.stream(q.split(" ")).allMatch(haystack::contains);
    }
}
