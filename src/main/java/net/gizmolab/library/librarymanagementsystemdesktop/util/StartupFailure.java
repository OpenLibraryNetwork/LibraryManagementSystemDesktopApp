package net.gizmolab.library.librarymanagementsystemdesktop.util;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Turns an exception that stopped the application from starting into a sentence for the librarian.
 * Spring wraps the real cause several levels deep, so the whole cause chain is examined.
 * H2 error codes: 90048 unsupported file version, 90020 database already open,
 * 28000 wrong user name or password and 90049 encryption error (the file does not open with this key).
 */
public final class StartupFailure {

    private StartupFailure() {}

    public static String message(Throwable error) {
        Throwable root = error;
        Set<Throwable> seen = new HashSet<>();
        for (Throwable t = error; t != null && seen.add(t); t = t.getCause()) {
            root = t;
            int code = t instanceof SQLException sql ? sql.getErrorCode() : 0;
            String text = t.getMessage() == null ? "" : t.getMessage();
            if (code == 90048 || text.contains("Unsupported database file version")) {
                return "Η τοπική βάση δεδομένων είναι από παλαιότερη έκδοση της εφαρμογής και δεν μπορεί να ανοίξει. "
                        + "Επικοινωνήστε με τον διαχειριστή.";
            }
            if (code == 90020 || text.contains("Database may be already in use")) {
                return "Η τοπική βάση δεδομένων χρησιμοποιείται ήδη: ίσως η εφαρμογή είναι ήδη ανοιχτή σε άλλο παράθυρο.";
            }
            if (code == 28000 || code == 90049) {
                return "Η τοπική βάση δεδομένων δεν ανοίγει με το κλειδί που είναι αποθηκευμένο σε αυτόν τον υπολογιστή. "
                        + "Επικοινωνήστε με τον διαχειριστή.";
            }
        }
        String detail = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
        return "Η εφαρμογή δεν μπόρεσε να ξεκινήσει. (Λεπτομέρειες: " + detail + ")";
    }
}
