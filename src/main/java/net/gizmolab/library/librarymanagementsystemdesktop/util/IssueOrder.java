package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Issues in reading order: by the first number of the issue number ("9" before "10"), then those without a number by period. */
public final class IssueOrder {

    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d{1,9})");

    public static final Comparator<PublicationDTO> NATURAL = Comparator
            .comparing((PublicationDTO p) -> firstNumber(p.getIssueNumber()) == null)
            .thenComparing(p -> firstNumber(p.getIssueNumber()), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(p -> SearchText.normalize(nz(p.getIssueNumber())))
            .thenComparing(p -> SearchText.normalize(nz(p.getPublicationMonthYear())))
            .thenComparing(p -> p.getId() == null ? 0L : p.getId());

    private IssueOrder() {}

    static Long firstNumber(String text) {
        if (text == null) return null;
        Matcher m = FIRST_NUMBER.matcher(text);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
