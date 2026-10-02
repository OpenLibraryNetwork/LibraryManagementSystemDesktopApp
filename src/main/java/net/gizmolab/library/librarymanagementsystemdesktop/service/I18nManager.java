package net.gizmolab.library.librarymanagementsystemdesktop.service;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Manager for internationalization (i18n) functionality.
 * Handles language switching and message retrieval for the JavaFX application.
 */
@Component
public class I18nManager {
    
    private static final String BUNDLE_BASE_NAME = "messages";
    private static final String LANGUAGE_PREFERENCE_KEY = "language";
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    
    private final ObjectProperty<Locale> currentLocale = new SimpleObjectProperty<>();
    private ResourceBundle currentBundle;
    private final Preferences preferences;
    
    public I18nManager() {
        this.preferences = Preferences.userNodeForPackage(I18nManager.class);
        
        // Load saved language preference or use default
        String savedLanguage = preferences.get(LANGUAGE_PREFERENCE_KEY, DEFAULT_LOCALE.getLanguage());
        Locale initialLocale = "el".equals(savedLanguage) ? Locale.of("el", "GR") : DEFAULT_LOCALE;
        
        // Eagerly load the resource bundle BEFORE setting the locale
        // This ensures currentBundle is immediately available for getMessage() calls
        // The listener will still work for future language changes
        loadResourceBundle(initialLocale);
        
        setLocale(initialLocale);
        
        // Listen for locale changes
        currentLocale.addListener((observable, oldLocale, newLocale) -> {
            loadResourceBundle(newLocale);
            saveLanguagePreference(newLocale);
        });
    }
    
    /**
     * Gets a localized message for the given key.
     * 
     * @param key the message key
     * @return the localized message, or the key itself if not found
     */
    public String getMessage(String key) {
        try {
            if (currentBundle == null) {
                return key;
            }
            return currentBundle.getString(key);
        } catch (Exception _) {
            // Return the key if message not found
            return key;
        }
    }
    
    /**
     * Gets a localized message with parameters.
     * 
     * @param key the message key
     * @param params the parameters to substitute
     * @return the formatted localized message
     */
    public String getMessage(String key, Object... params) {
        try {
            String message = currentBundle.getString(key);
            return MessageFormat.format(message, params);
        } catch (Exception _) {
            // Return the key if message not found
            return key;
        }
    }
    
    /**
     * Changes the current locale.
     * 
     * @param locale the new locale
     */
    public void setLocale(Locale locale) {
        currentLocale.set(locale);
    }
    
    /**
     * Gets the current locale.
     * 
     * @return the current locale
     */
    public Locale getCurrentLocale() {
        return currentLocale.get();
    }
    
    /**
     * Gets the current locale property for binding.
     * 
     * @return the locale property
     */
    public ObjectProperty<Locale> currentLocaleProperty() {
        return currentLocale;
    }
    
    /**
     * Checks if the current language is Greek.
     * 
     * @return true if current language is Greek
     */
    public boolean isGreek() {
        return "el".equals(getCurrentLocale().getLanguage());
    }
    
    /**
     * Checks if the current language is English.
     * 
     * @return true if current language is English
     */
    public boolean isEnglish() {
        return "en".equals(getCurrentLocale().getLanguage());
    }
    
    /**
     * Gets available locales.
     * 
     * @return array of available locales
     */
    public Locale[] getAvailableLocales() {
        return new Locale[] {
            Locale.ENGLISH,
            Locale.of("el", "GR")
        };
    }
    
    /**
     * Gets display name for a locale.
     * 
     * @param locale the locale
     * @return the display name
     */
    public String getDisplayName(Locale locale) {
        if ("el".equals(locale.getLanguage())) {
            return "Ελληνικά";
        } else {
            return "English";
        }
    }
    
    private void loadResourceBundle(Locale locale) {
        try {
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
        } catch (Exception _) {
            try {
                // Fallback to default locale
                currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, DEFAULT_LOCALE);
            } catch (Exception _) {
                currentBundle = null;
            }
        }
    }
    
    private void saveLanguagePreference(Locale locale) {
        preferences.put(LANGUAGE_PREFERENCE_KEY, locale.getLanguage());
    }
}