package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import javafx.beans.property.ObjectProperty;
import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Tests for i18n Translation Fix
 * 
 * **CRITICAL**: These tests MUST FAIL on unfixed code - failure confirms the bugs exist
 * **DO NOT attempt to fix the tests or the code when they fail**
 * **NOTE**: These tests encode the expected behavior - they will validate the fix when they pass after implementation
 * **GOAL**: Surface counterexamples that demonstrate the three bugs exist
 */
class I18nTranslationBugfixTest {

    /**
     * Property 1.1: Fault Condition - Immediate Bundle Availability
     * 
     * **Validates: Requirements 2.1, 2.2, 2.3**
     * 
     * Tests that immediately after I18nManager construction, getMessage() returns
     * translated text instead of translation keys.
     * 
     * **EXPECTED ON UNFIXED CODE**: This test WILL FAIL because currentBundle is null
     * immediately after construction, causing getMessage() to return the key itself.
     * 
     * **Counterexample Expected**: getMessage("app.title") returns "app.title" instead of "Library System"
     */
    @Property(tries = 3)
    @Label("Property 1.1: Immediate getMessage after construction returns translated text, not keys")
    void immediateGetMessageReturnsTranslatedText(@ForAll("translationKeys") String key) {
        // Create a new I18nManager instance
        I18nManager i18nManager = new I18nManager();
        
        // Immediately call getMessage (simulating UI initialization)
        String result = i18nManager.getMessage(key);
        
        // The result should NOT be the key itself - it should be translated text
        // On unfixed code, this will fail because currentBundle is null
        assertNotEquals(key, result, 
            "getMessage() should return translated text, not the key itself. " +
            "This indicates currentBundle is null immediately after construction.");
        
        // The result should not be empty
        assertFalse(result.isEmpty(), 
            "getMessage() should return non-empty translated text");
    }

    /**
     * Provides common translation keys used in the application
     */
    @Provide
    Arbitrary<String> translationKeys() {
        return Arbitraries.of(
            "app.title",
            "navigation.books",
            "navigation.users",
            "common.save",
            "common.cancel",
            "common.delete",
            "book.addBook",
            "user.addUser"
        );
    }

    /**
     * Property 1.2: Fault Condition - Greek Character Display
     * 
     * **Validates: Requirements 2.4, 2.5**
     * 
     * Tests that Greek locale displays proper Greek characters, not corrupted characters (?)
     * 
     * **EXPECTED ON UNFIXED CODE**: This test WILL FAIL because messages_el.properties
     * has corrupted encoding, causing Greek characters to appear as question marks.
     * 
     * **Counterexample Expected**: Greek text contains '?' characters instead of proper Greek letters
     */
    @Property(tries = 3)
    @Label("Property 1.2: Greek locale displays proper Greek characters, not corrupted")
    void greekLocaleDisplaysProperCharacters(@ForAll("greekTranslationKeys") String key) {
        // Create I18nManager and set Greek locale
        I18nManager i18nManager = new I18nManager();
        i18nManager.setLocale(Locale.of("el", "GR"));
        
        // Small delay to ensure listener execution completes
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String result = i18nManager.getMessage(key);
        
        // Greek text should not contain question marks (indicating corrupted encoding)
        assertFalse(result.contains("?"), 
            "Greek translation should not contain '?' characters. " +
            "This indicates encoding corruption in messages_el.properties. " +
            "Got: " + result);
        
        // Greek text should contain at least one Greek character (Unicode range 0x0370-0x03FF)
        boolean containsGreekChar = result.chars()
            .anyMatch(c -> (c >= 0x0370 && c <= 0x03FF));
        
        assertTrue(containsGreekChar, 
            "Greek translation should contain Greek characters. " +
            "Got: " + result);
    }

    /**
     * Provides translation keys that should have Greek text
     */
    @Provide
    Arbitrary<String> greekTranslationKeys() {
        return Arbitraries.of(
            "app.title",
            "navigation.books",
            "common.save",
            "book.title",
            "user.firstName"
        );
    }

    /**
     * Property 1.3: Fault Condition - Fallback Bundle Availability
     * 
     * **Validates: Requirements 2.6**
     * 
     * Tests that a default fallback bundle (messages.properties) exists
     * 
     * **EXPECTED ON UNFIXED CODE**: This test WILL FAIL because messages.properties
     * does not exist, leaving no ultimate fallback when locale-specific bundles fail.
     * 
     * **Counterexample Expected**: messages.properties file does not exist
     */
    @Test
    @Label("Test 1.3: Default fallback bundle messages.properties exists")
    void defaultFallbackBundleExists() {
        // Check if messages.properties file exists in resources
        File messagesFile = new File("src/main/resources/messages.properties");
        
        assertTrue(messagesFile.exists(), 
            "Default fallback bundle messages.properties should exist. " +
            "This provides ultimate fallback when locale-specific bundles fail.");
        
        // Verify it can be loaded as a ResourceBundle
        ResourceBundle bundle = ResourceBundle.getBundle("messages");
        assertNotNull(bundle, "Default bundle should be loadable");
        
        // Verify it contains at least some keys
        assertTrue(bundle.containsKey("app.title"), 
            "Default bundle should contain common keys like 'app.title'");
    }

    /**
     * Additional Test: Verify Greek properties file encoding
     * 
     * This test directly checks the file encoding to confirm the root cause
     */
    @Test
    @Label("Test 1.4: Greek properties file has UTF-8 encoding")
    void greekPropertiesFileHasUTF8Encoding() throws IOException {
        File greekPropsFile = new File("src/main/resources/messages_el.properties");
        assertTrue(greekPropsFile.exists(), "Greek properties file should exist");
        
        // Try to read the file with UTF-8 encoding
        Properties props = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(greekPropsFile), StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        
        // Check a known Greek key
        String appTitle = props.getProperty("app.title");
        assertNotNull(appTitle, "app.title should exist in Greek properties");
        
        // Should not contain question marks (corruption indicator)
        assertFalse(appTitle.contains("?"), 
            "Greek text should not contain '?' characters indicating corruption. " +
            "Got: " + appTitle);
        
        // Should contain Greek characters
        boolean containsGreekChar = appTitle.chars()
            .anyMatch(c -> (c >= 0x0370 && c <= 0x03FF));
        
        assertTrue(containsGreekChar, 
            "Greek text should contain actual Greek characters. " +
            "Got: " + appTitle);
            
    }

    /**
     * Additional Test: Immediate bundle availability (unit test version)
     * 
     * This is a concrete unit test that complements the property-based test
     */
    @Test
    @Label("Test 1.5: Immediate getMessage after construction works correctly")
    void immediateGetMessageAfterConstruction() {
        // Create a new I18nManager instance
        I18nManager i18nManager = new I18nManager();
        
        // Immediately call getMessage without any delay or locale change
        String appTitle = i18nManager.getMessage("app.title");
        
        // Should return translated text, not the key
        assertNotEquals("app.title", appTitle, 
            "getMessage() should return translated text immediately after construction, not the key. " +
            "This indicates currentBundle is null. Got: " + appTitle);
        
        // Should return actual content
        assertFalse(appTitle.isEmpty(), 
            "getMessage() should return non-empty content");
        
        // For English default, should be "Library System"
        // For Greek saved preference, should be Greek text
        assertTrue(appTitle.equals("Library System") || appTitle.contains("Σύστημα") || appTitle.contains("?"),
            "Should return either English or Greek translation (or corrupted Greek). Got: " + appTitle);
    }

    // ========================================================================
    // PRESERVATION PROPERTY TESTS
    // ========================================================================
    // These tests verify that existing functionality remains unchanged after the fix.
    // **EXPECTED ON UNFIXED CODE**: These tests SHOULD PASS
    // **EXPECTED ON FIXED CODE**: These tests SHOULD STILL PASS (no regressions)
    // ========================================================================

    /**
     * Property 2.1: Preservation - Language Switching Through UI
     * 
     * **Validates: Requirements 3.1, 3.2**
     * 
     * Tests that language switching through setLocale() after initialization works correctly.
     * This functionality should remain unchanged after the fix.
     * 
     * **EXPECTED ON UNFIXED CODE**: This test SHOULD PASS
     * **EXPECTED ON FIXED CODE**: This test SHOULD STILL PASS
     * 
     * Note: This test switches locale TWICE to ensure the bundle is loaded.
     * On unfixed code, the first setLocale() in constructor doesn't load the bundle,
     * but subsequent setLocale() calls DO work correctly.
     */
    @Property(tries = 2)
    @Label("Property 2.1: Language switching through setLocale() works correctly")
    void languageSwitchingWorksCorrectly(@ForAll("availableLocales") Locale targetLocale) {
        // Create I18nManager (first setLocale in constructor may not load bundle)
        I18nManager i18nManager = new I18nManager();
        
        // Switch to opposite locale first (to ensure we're testing a real change)
        Locale oppositeLocale = "en".equals(targetLocale.getLanguage()) ? 
            Locale.of("el", "GR") : Locale.ENGLISH;
        i18nManager.setLocale(oppositeLocale);
        
        // Wait for listener to execute
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Now switch to target locale (this is the behavior we're testing)
        i18nManager.setLocale(targetLocale);
        
        // Wait for listener to execute
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify the locale was changed
        assertEquals(targetLocale.getLanguage(), i18nManager.getCurrentLocale().getLanguage(),
            "Current locale should match the target locale after setLocale()");
        
        // Verify getMessage works after locale change
        String result = i18nManager.getMessage("app.title");
        assertNotNull(result, "getMessage() should return non-null after locale change");
        assertFalse(result.isEmpty(), "getMessage() should return non-empty after locale change");
        
        // After a manual locale change (not the initial one), the bundle should be loaded
        // and getMessage should return translated text, not the key
        assertNotEquals("app.title", result, 
            "After manual locale change, getMessage() should return translated text, not the key");
    }

    /**
     * Provides available locales for testing
     */
    @Provide
    Arbitrary<Locale> availableLocales() {
        return Arbitraries.of(
            Locale.ENGLISH,
            Locale.of("el", "GR")
        );
    }

    /**
     * Property 2.2: Preservation - Preference Save/Load
     * 
     * **Validates: Requirements 3.5, 3.6**
     * 
     * Tests that preference storage and loading works correctly.
     * This functionality should remain unchanged after the fix.
     * 
     * **EXPECTED ON UNFIXED CODE**: This test SHOULD PASS
     * **EXPECTED ON FIXED CODE**: This test SHOULD STILL PASS
     */
    @Property(tries = 2)
    @Label("Property 2.2: Preference save/load works correctly")
    void preferenceSaveLoadWorksCorrectly(@ForAll("availableLocales") Locale locale) {
        // Create first I18nManager and set locale
        I18nManager i18nManager1 = new I18nManager();
        i18nManager1.setLocale(locale);
        
        // Wait for preference to be saved
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Create second I18nManager (simulating app restart)
        I18nManager i18nManager2 = new I18nManager();
        
        // Wait for initialization
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify the saved locale was loaded
        assertEquals(locale.getLanguage(), i18nManager2.getCurrentLocale().getLanguage(),
            "Newly created I18nManager should load the saved locale preference");
    }

    /**
     * Property 2.3: Preservation - Missing Key Fallback
     * 
     * **Validates: Requirements 3.3**
     * 
     * Tests that missing key fallback returns the key itself.
     * This functionality should remain unchanged after the fix.
     * 
     * **EXPECTED ON UNFIXED CODE**: This test SHOULD PASS
     * **EXPECTED ON FIXED CODE**: This test SHOULD STILL PASS
     */
    @Property(tries = 3)
    @Label("Property 2.3: Missing key fallback returns the key itself")
    void missingKeyFallbackWorksCorrectly(@ForAll("invalidKeys") String invalidKey) {
        // Create I18nManager
        I18nManager i18nManager = new I18nManager();
        
        // Ensure bundle is loaded by switching locale
        i18nManager.setLocale(Locale.ENGLISH);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Try to get message for invalid key
        String result = i18nManager.getMessage(invalidKey);
        
        // Should return the key itself as fallback
        assertEquals(invalidKey, result,
            "getMessage() should return the key itself when the key is not found");
    }

    /**
     * Provides invalid/missing translation keys
     */
    @Provide
    Arbitrary<String> invalidKeys() {
        return Arbitraries.of(
            "nonexistent.key",
            "invalid.translation",
            "missing.message",
            "unknown.key.here",
            "this.does.not.exist"
        );
    }

    /**
     * Property 2.4: Preservation - MessageFormat with Parameters
     * 
     * **Validates: Requirements 3.4**
     * 
     * Tests that MessageFormat with parameters works correctly.
     * This functionality should remain unchanged after the fix.
     * 
     * **EXPECTED ON UNFIXED CODE**: This test SHOULD PASS
     * **EXPECTED ON FIXED CODE**: This test SHOULD STILL PASS
     */
    @Test
    @Label("Test 2.4: MessageFormat with parameters works correctly")
    void messageFormatWithParametersWorksCorrectly() {
        // Create I18nManager
        I18nManager i18nManager = new I18nManager();
        
        // Ensure bundle is loaded
        i18nManager.setLocale(Locale.ENGLISH);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test getMessage with parameters
        // Even if the key doesn't have parameters, it should not throw an exception
        String result1 = i18nManager.getMessage("app.title", 42, "test");
        assertNotNull(result1, "getMessage() with parameters should return non-null");
        assertFalse(result1.isEmpty(), "getMessage() with parameters should return non-empty");
        
        // Test with different parameter types
        String result2 = i18nManager.getMessage("common.save", 100, "param");
        assertNotNull(result2, "getMessage() with parameters should return non-null");
        assertFalse(result2.isEmpty(), "getMessage() with parameters should return non-empty");
        
        // The method should handle parameters gracefully even if the message doesn't use them
        // This tests that the MessageFormat functionality is preserved
    }

    /**
     * Property 2.5: Preservation - English Locale Display
     * 
     * **Validates: Requirements 3.7**
     * 
     * Tests that English locale displays correctly after a manual locale change.
     * This functionality should remain unchanged after the fix.
     * 
     * **EXPECTED ON UNFIXED CODE**: This test SHOULD PASS
     * **EXPECTED ON FIXED CODE**: This test SHOULD STILL PASS
     * 
     * Note: This test switches locale TWICE to ensure the bundle is loaded.
     * On unfixed code, the first setLocale() in constructor doesn't load the bundle,
     * but subsequent setLocale() calls DO work correctly.
     */
    @Property(tries = 3)
    @Label("Property 2.5: English locale displays correctly after manual change")
    void englishLocaleDisplaysCorrectly(@ForAll("translationKeys") String key) {
        // Create I18nManager
        I18nManager i18nManager = new I18nManager();
        
        // Switch to Greek first (to ensure we're testing a real change)
        i18nManager.setLocale(Locale.of("el", "GR"));
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Now switch to English (this is the behavior we're testing)
        i18nManager.setLocale(Locale.ENGLISH);
        
        // Wait for locale change
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get message for the key
        String result = i18nManager.getMessage(key);
        
        // Should return non-null, non-empty result
        assertNotNull(result, "getMessage() should return non-null for English locale");
        assertFalse(result.isEmpty(), "getMessage() should return non-empty for English locale");
        
        // After manual locale change, should return translated text, not the key
        assertNotEquals(key, result, 
            "getMessage() should return translated text, not the key, after English locale is manually set");
        
        // English text should not contain Greek characters
        boolean containsGreekChar = result.chars()
            .anyMatch(c -> (c >= 0x0370 && c <= 0x03FF));
        
        assertFalse(containsGreekChar, 
            "English translation should not contain Greek characters. Got: " + result);
    }

    /**
     * Additional Test: Verify helper methods remain unchanged
     * 
     * Tests that utility methods like isGreek(), isEnglish(), getAvailableLocales(), etc.
     * continue to work correctly.
     */
    @Test
    @Label("Test 2.6: Helper methods work correctly")
    void helperMethodsWorkCorrectly() {
        I18nManager i18nManager = new I18nManager();
        
        // Test with English locale
        i18nManager.setLocale(Locale.ENGLISH);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue(i18nManager.isEnglish(), "isEnglish() should return true for English locale");
        assertFalse(i18nManager.isGreek(), "isGreek() should return false for English locale");
        
        // Test with Greek locale
        i18nManager.setLocale(Locale.of("el", "GR"));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue(i18nManager.isGreek(), "isGreek() should return true for Greek locale");
        assertFalse(i18nManager.isEnglish(), "isEnglish() should return false for Greek locale");
        
        // Test getAvailableLocales
        Locale[] availableLocales = i18nManager.getAvailableLocales();
        assertNotNull(availableLocales, "getAvailableLocales() should return non-null");
        assertEquals(2, availableLocales.length, "Should have 2 available locales");
        
        // Test getDisplayName
        assertEquals("English", i18nManager.getDisplayName(Locale.ENGLISH),
            "Display name for English should be 'English'");
        assertEquals("Ελληνικά", i18nManager.getDisplayName(Locale.of("el", "GR")),
            "Display name for Greek should be 'Ελληνικά'");
    }

    /**
     * Additional Test: Verify currentLocaleProperty binding works
     * 
     * Tests that the JavaFX property binding functionality remains unchanged.
     */
    @Test
    @Label("Test 2.7: CurrentLocaleProperty binding works correctly")
    void currentLocalePropertyBindingWorks() {
        I18nManager i18nManager = new I18nManager();
        
        // Get the property
        ObjectProperty<Locale> localeProperty = i18nManager.currentLocaleProperty();
        assertNotNull(localeProperty, "currentLocaleProperty() should return non-null");
        
        // Track changes
        final boolean[] listenerCalled = {false};
        localeProperty.addListener((observable, oldValue, newValue) -> {
            listenerCalled[0] = true;
        });
        
        // Change locale
        i18nManager.setLocale(Locale.of("el", "GR"));
        
        // Wait for listener
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify listener was called
        assertTrue(listenerCalled[0], "Locale property listener should be called when locale changes");
        
        // Verify property value matches
        assertEquals("el", localeProperty.get().getLanguage(),
            "Property value should match the set locale");
    }
}
