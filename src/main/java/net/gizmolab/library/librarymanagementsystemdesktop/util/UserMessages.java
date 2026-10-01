package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;

import java.net.ConnectException;

/**
 * Turns exceptions into short Greek messages for the user.
 * Walks the cause chain, because background Tasks wrap the original exception.
 */
public final class UserMessages {

    public static final String FORBIDDEN = "Η ενέργεια δεν επιτρέπεται για τον λογαριασμό σας.";
    public static final String SESSION_EXPIRED = "Η σύνδεση έληξε. Συνδεθείτε ξανά.";
    public static final String CONFLICT = "Η ενέργεια δεν ολοκληρώθηκε λόγω σύγκρουσης (π.χ. το αντίτυπο είναι ήδη δανεισμένο).";
    public static final String UNREACHABLE = "Δεν υπάρχει σύνδεση με τον server.";
    public static final String GENERIC = "Η ενέργεια απέτυχε.";

    private UserMessages() {}

    public static String describe(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof StrapiApiClient.ForbiddenException) return FORBIDDEN;
            if (t instanceof StrapiApiClient.AuthenticationExpiredException) return SESSION_EXPIRED;
            if (t instanceof StrapiApiClient.ConflictException) return CONFLICT;
            // Only messages written for users: application errors (400) and Biblionet limits/outages (429, 502).
            // 404/500 carry generic English texts; 409 is handled above as CONFLICT.
            if (t instanceof StrapiApiClient.StrapiApiException api && api.getServerMessage() != null
                    && (api.getStatus() == 400 || api.getStatus() == 429 || api.getStatus() == 502)) {
                return api.getServerMessage();
            }
            if (t instanceof ConnectException) return UNREACHABLE;
        }
        return GENERIC;
    }
}
