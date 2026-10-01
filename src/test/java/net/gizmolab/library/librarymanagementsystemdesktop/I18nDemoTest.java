package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify i18n functionality works correctly.
 */
class I18nDemoTest {

    @Test
    void testI18nSystemFunctionality() {
        I18nManager i18nManager = new I18nManager();
        
        // Test English
        i18nManager.setLocale(Locale.ENGLISH);
        assertEquals("Library System", i18nManager.getMessage("app.title"));
        assertEquals("Books", i18nManager.getMessage("navigation.books"));
        assertEquals("Save", i18nManager.getMessage("common.save"));
        assertEquals("Books loaded successfully", i18nManager.getMessage("status.module.loaded", "Books"));
        assertTrue(i18nManager.isEnglish());
        assertFalse(i18nManager.isGreek());
        
        // Test Greek
        i18nManager.setLocale(Locale.of("el", "GR"));
        assertEquals("Σύστημα Βιβλιοθήκης", i18nManager.getMessage("app.title"));
        assertEquals("Βιβλία", i18nManager.getMessage("navigation.books"));
        assertEquals("Αποθήκευση", i18nManager.getMessage("common.save"));
        assertEquals("Βιβλία φορτώθηκε επιτυχώς", i18nManager.getMessage("status.module.loaded", "Βιβλία"));
        assertFalse(i18nManager.isEnglish());
        assertTrue(i18nManager.isGreek());
        
        // Test available locales
        Locale[] locales = i18nManager.getAvailableLocales();
        assertEquals(2, locales.length);
        
        // Test display names
        assertEquals("English", i18nManager.getDisplayName(Locale.ENGLISH));
        assertEquals("Ελληνικά", i18nManager.getDisplayName(Locale.of("el", "GR")));
        
        // Test missing key fallback
        assertEquals("nonexistent.key", i18nManager.getMessage("nonexistent.key"));
    }
}