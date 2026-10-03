package net.gizmolab.library.librarymanagementsystemdesktop.service.exceptions;

/** A borrower who still has a book at home cannot be deleted: nobody would know who has the copy. */
public class ActiveBorrowsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public static final String MESSAGE =
            "Ο δανειζόμενος έχει ακόμα δανεισμένα αντίτυπα. Καταχωρίστε πρώτα την επιστροφή τους.";

    public ActiveBorrowsException() {
        super(MESSAGE);
    }
}
