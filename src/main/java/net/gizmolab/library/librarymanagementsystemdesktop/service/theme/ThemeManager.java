package net.gizmolab.library.librarymanagementsystemdesktop.service.theme;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton manager for application theme operations.
 * Handles theme loading, switching, and preference persistence.
 */
/**
 * Singleton manager for application theme operations using AtlantaFX.
 * Handles theme loading, switching, and preference persistence.
 */
public class ThemeManager {
    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());
    private static final String PREF_THEME_KEY = "selectedTheme";
    private static final String PREF_ANIMATIONS_KEY = "animationsEnabled";

    private static ThemeManager instance;
    private Scene scene;
    private AtlantaFXTheme currentTheme;
    private ThemePreference themePreference;
    private final Preferences preferences;

    private ThemeManager() {
        this.preferences = Preferences.userNodeForPackage(ThemeManager.class);
        this.themePreference = new ThemePreference();
    }

    /**
     * Gets the singleton instance of ThemeManager.
     *
     * @return the ThemeManager instance
     */
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Initializes the ThemeManager with a JavaFX Scene reference.
     * This should be called after the theme has been loaded but before showing the stage.
     * The scene reference is needed for future theme switches.
     *
     * @param scene the JavaFX Scene to apply themes to
     */
    public void initialize(Scene scene) {
        this.scene = scene;
        // Don't call loadSavedTheme() here - it should be called BEFORE creating the Scene
        // so that Application.setUserAgentStylesheet() is set before any nodes are created
    }
    /**
     * Sets the current theme without applying it.
     * This is used when the theme has been applied manually (e.g., in Application.start()).
     *
     * @param theme the theme that is currently active
     */
    public void setCurrentTheme(AtlantaFXTheme theme) {
        this.currentTheme = theme;
        this.themePreference.setSelectedTheme(theme);
    }

    /**
     * Applies the specified AtlantaFX theme to the application.
     * MODIFIED: Now uses custom CSS instead of AtlantaFX themes.
     *
     * @param theme the AtlantaFX theme to apply (ignored, kept for compatibility)
     * @throws ThemeLoadException if theme loading fails
     */
    public void applyTheme(AtlantaFXTheme theme) throws ThemeLoadException {
        if (scene == null) {
            LOGGER.warning("ThemeManager not initialized. Call initialize(Scene) first.");
            throw new ThemeLoadException("ThemeManager not initialized");
        }

        long startTime = System.currentTimeMillis();

        try {
            // CUSTOM CSS APPROACH - Don't use AtlantaFX
            // Keep User Agent Stylesheet as null (no default theme)
            javafx.application.Application.setUserAgentStylesheet(null);
            
            // Apply custom CSS
            String customCss = getClass().getResource("/css/modern-theme.css").toExternalForm();
            if (!scene.getStylesheets().contains(customCss)) {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(customCss);
            }

            currentTheme = theme;

            long elapsedTime = System.currentTimeMillis() - startTime;
            LOGGER.info(String.format("Applied custom theme in %dms", elapsedTime));

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            LOGGER.log(Level.SEVERE, "Failed to apply custom theme after " + elapsedTime + "ms", e);
            throw new ThemeLoadException("Critical: Cannot load custom theme", e);
        }
    }

    /**
     * Switches to the specified theme with a smooth transition animation.
     * Coordinates the theme change with visual feedback.
     * Monitors performance to ensure switching completes without visible lag.
     *
     * @param theme the theme to switch to
     */
    public void switchTheme(AtlantaFXTheme theme) {
        if (scene == null) {
            LOGGER.warning("ThemeManager not initialized. Call initialize(Scene) first.");
            return;
        }

        if (theme == currentTheme) {
            LOGGER.info("Theme already active: " + theme.getDisplayName());
            return;
        }

        long startTime = System.currentTimeMillis();

        // Apply fade transition if animations are enabled
        if (themePreference.isAnimationsEnabled()) {
            FadeTransition fadeOut = new FadeTransition(StyleConstants.ANIMATION_FAST, scene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.95);

            fadeOut.setOnFinished(event -> {
                try {
                    applyTheme(theme);
                    saveThemePreference(theme);

                    long elapsedTime = System.currentTimeMillis() - startTime;
                    LOGGER.info(String.format("Theme switch completed in %dms", elapsedTime));

                    // Validate performance - theme switching should not cause visible lag
                    if (elapsedTime > 1000) {
                        LOGGER.warning(String.format("Theme switching took %dms, may cause visible lag", elapsedTime));
                    }
                } catch (ThemeLoadException e) {
                    LOGGER.log(Level.SEVERE, "Failed to switch theme", e);
                }

                FadeTransition fadeIn = new FadeTransition(StyleConstants.ANIMATION_FAST, scene.getRoot());
                fadeIn.setFromValue(0.95);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();
        } else {
            try {
                applyTheme(theme);
                saveThemePreference(theme);

                long elapsedTime = System.currentTimeMillis() - startTime;
                LOGGER.info(String.format("Theme switch completed in %dms (no animation)", elapsedTime));

                if (elapsedTime > 500) {
                    LOGGER.warning(String.format("Theme switching took %dms without animation, exceeding 500ms target", elapsedTime));
                }
            } catch (ThemeLoadException e) {
                LOGGER.log(Level.SEVERE, "Failed to switch theme", e);
            }
        }
    }

    /**
     * Gets the currently active theme.
     *
     * @return the current theme, or null if no theme is applied
     */
    public AtlantaFXTheme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Gets the list of all available AtlantaFX themes.
     *
     * @return list of available themes
     */
    public List<AtlantaFXTheme> getAvailableThemes() {
        return Arrays.asList(AtlantaFXTheme.values());
    }

    /**
     * Loads the saved theme preference and applies it.
     * If no preference is saved or if the saved preference is invalid,
     * applies the default theme (PrimerLight).
     */
    /**
     * Loads the saved theme preference from persistent storage and applies it.
     * If no preference exists, applies the default theme (PrimerLight).
     * This should be called BEFORE creating any Scene to ensure proper styling.
     */
    public void loadSavedTheme() {
        try {
            String savedThemeName = preferences.get(PREF_THEME_KEY, null);
            boolean animationsEnabled = preferences.getBoolean(PREF_ANIMATIONS_KEY, true);

            themePreference.setAnimationsEnabled(animationsEnabled);

            if (savedThemeName == null) {
                // No saved preference, use default
                LOGGER.info("No saved theme preference, using default (PrimerLight)");
                themePreference.setSelectedTheme(AtlantaFXTheme.PRIMER_LIGHT);
                applyThemeWithoutScene(AtlantaFXTheme.PRIMER_LIGHT);
                return;
            }

            AtlantaFXTheme savedTheme = AtlantaFXTheme.valueOf(savedThemeName);
            themePreference.setSelectedTheme(savedTheme);
            applyThemeWithoutScene(savedTheme);

            LOGGER.info("Loaded saved theme: " + savedTheme.getDisplayName());

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid saved theme preference, using default (PrimerLight)", e);

            // Clear invalid preference
            preferences.remove(PREF_THEME_KEY);

            themePreference.setSelectedTheme(AtlantaFXTheme.PRIMER_LIGHT);
            applyThemeWithoutScene(AtlantaFXTheme.PRIMER_LIGHT);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load saved theme, using default (PrimerLight)", e);
            themePreference.setSelectedTheme(AtlantaFXTheme.PRIMER_LIGHT);
            applyThemeWithoutScene(AtlantaFXTheme.PRIMER_LIGHT);
        }
    }

    /**
     * Applies a theme without requiring a Scene reference.
     * MODIFIED: Now uses custom CSS instead of AtlantaFX.
     *
     * @param theme the theme to apply (ignored, kept for compatibility)
     */
    private void applyThemeWithoutScene(AtlantaFXTheme theme) {
        try {
            // CUSTOM CSS APPROACH - Don't use AtlantaFX
            javafx.application.Application.setUserAgentStylesheet(null);
            currentTheme = theme;
            LOGGER.info("Set User Agent Stylesheet to null for custom CSS");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set User Agent Stylesheet to null", e);
        }
    }

    /**
     * Saves the theme preference for persistence across sessions.
     *
     * @param theme the theme to save as preferred
     */
    public void saveThemePreference(AtlantaFXTheme theme) {
        try {
            preferences.put(PREF_THEME_KEY, theme.name());
            themePreference.setSelectedTheme(theme);
            LOGGER.info("Saved theme preference: " + theme.getDisplayName());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save theme preference", e);
        }
    }

    /**
     * Gets the current theme preference settings.
     *
     * @return the theme preference object
     */
    public ThemePreference getThemePreference() {
        return themePreference;
    }

    /**
     * Updates the animation enabled setting and saves it.
     *
     * @param enabled true to enable animations, false to disable
     */
    public void setAnimationsEnabled(boolean enabled) {
        themePreference.setAnimationsEnabled(enabled);
        preferences.putBoolean(PREF_ANIMATIONS_KEY, enabled);
        LOGGER.info("Animations " + (enabled ? "enabled" : "disabled"));
    }
}

