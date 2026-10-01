package net.gizmolab.library.librarymanagementsystemdesktop.util;

import java.util.OptionalInt;

/** Number of copies typed in the add wizards: a whole number from 1 to 50. */
public final class CopyCount {

    public static final int MIN = 1;
    public static final int MAX = 50;

    private CopyCount() {}

    public static OptionalInt parse(String text) {
        if (text == null || !text.trim().matches("\\d{1,3}")) return OptionalInt.empty();
        int value = Integer.parseInt(text.trim());
        return value >= MIN && value <= MAX ? OptionalInt.of(value) : OptionalInt.empty();
    }
}
