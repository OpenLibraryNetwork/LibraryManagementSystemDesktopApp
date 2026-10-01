package net.gizmolab.library.librarymanagementsystemdesktop.controller.base;

import org.junit.jupiter.api.Test;

import java.util.ListResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** "Σύνολο: {0} στοιχεία" under every list that does not override getLocalizedMessage. */
class LocalizedMessageTest {

    private static class Probe extends BaseController {
        String message(String key, Object... args) {
            return getLocalizedMessage(key, args);
        }
    }

    @Test
    void placeholdersAreFilledIn() {
        Probe probe = new Probe();
        probe.setResources(new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][]{
                        {"status.items.total", "Σύνολο: {0} στοιχεία"},
                        {"status.items.filtered", "Εμφάνιση {0} από {1} στοιχεία"},
                };
            }
        });
        assertEquals("Σύνολο: 12 στοιχεία", probe.message("status.items.total", 12));
        assertEquals("Εμφάνιση 3 από 12 στοιχεία", probe.message("status.items.filtered", 3, 12));
        assertEquals("unknown.key", probe.message("unknown.key", 1));
    }
}
