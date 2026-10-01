package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMessagesTest {

    @Test
    void forbiddenEvenWhenWrapped() {
        Exception wrapped = new RuntimeException("task failed",
                new StrapiApiClient.ForbiddenException("{}", "Forbidden"));
        assertEquals(UserMessages.FORBIDDEN, UserMessages.describe(wrapped));
    }

    @Test
    void serverMessageIsShownAsIs() {
        Exception e = new StrapiApiClient.StrapiApiException(400, "{}", "Το αντίτυπο είναι δανεισμένο.");
        assertEquals("Το αντίτυπο είναι δανεισμένο.", UserMessages.describe(e));
    }

    @Test
    void strapiErrorWithoutMessageIsGeneric() {
        assertEquals(UserMessages.GENERIC, UserMessages.describe(new StrapiApiClient.StrapiApiException(500, "x", null)));
    }

    @Test
    void sessionConflictAndConnection() {
        assertEquals(UserMessages.SESSION_EXPIRED,
                UserMessages.describe(new StrapiApiClient.AuthenticationExpiredException("expired")));
        assertEquals(UserMessages.CONFLICT,
                UserMessages.describe(new StrapiApiClient.ConflictException("{}", null)));
        assertEquals(UserMessages.UNREACHABLE,
                UserMessages.describe(new RuntimeException(new ConnectException("refused"))));
    }

    @Test
    void anythingElseIsGeneric() {
        assertEquals(UserMessages.GENERIC, UserMessages.describe(new IllegalStateException("boom")));
    }

    @Test
    void onlyApplicationErrorsShowTheServerText() { // review #3: no raw English for 404/500
        assertEquals(UserMessages.GENERIC,
                UserMessages.describe(new StrapiApiClient.StrapiApiException(500, "{}", "Internal Server Error")));
        assertEquals(UserMessages.GENERIC,
                UserMessages.describe(new StrapiApiClient.StrapiApiException(404, "{}", "Not Found")));
    }

    @Test
    void biblionetLimitsShowTheServerText() {
        assertEquals("Ημερήσιο όριο Biblionet API (900 κλήσεις). Δοκιμάστε αύριο.",
                UserMessages.describe(new StrapiApiClient.StrapiApiException(429, "{}", "Ημερήσιο όριο Biblionet API (900 κλήσεις). Δοκιμάστε αύριο.")));
        assertEquals("Η Biblionet δεν απάντησε σωστά (timeout). Δοκιμάστε ξανά αργότερα.",
                UserMessages.describe(new StrapiApiClient.StrapiApiException(502, "{}", "Η Biblionet δεν απάντησε σωστά (timeout). Δοκιμάστε ξανά αργότερα.")));
    }

    @Test
    void conflictStaysGenericBecauseBorrowConflictsAreEnglish() {
        assertEquals(UserMessages.CONFLICT,
                UserMessages.describe(new StrapiApiClient.ConflictException("{}", "Copy is already borrowed or does not exist")));
    }
}
