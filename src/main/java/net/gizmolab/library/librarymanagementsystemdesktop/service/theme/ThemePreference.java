package net.gizmolab.library.librarymanagementsystemdesktop.service.theme;

/**
 * Data model representing user's theme preferences.
 * Stores the selected theme, animation settings, and optional accent color customization.
 */
/**
 * Data model representing user's theme preferences.
 * Stores the selected AtlantaFX theme, animation settings, and optional accent color customization.
 */
public class ThemePreference {
    private AtlantaFXTheme selectedTheme;
    private boolean animationsEnabled;
    private String accentColor; // Optional for future customization

    /**
     * Creates a new ThemePreference with default values.
     * Defaults: PRIMER_LIGHT theme, animations enabled.
     */
    public ThemePreference() {
        this.selectedTheme = AtlantaFXTheme.PRIMER_LIGHT;
        this.animationsEnabled = true;
    }

    /**
     * Gets the selected theme.
     *
     * @return the selected AtlantaFX theme
     */
    public AtlantaFXTheme getSelectedTheme() {
        return selectedTheme;
    }

    /**
     * Sets the selected theme.
     *
     * @param selectedTheme the AtlantaFX theme to set
     */
    public void setSelectedTheme(AtlantaFXTheme selectedTheme) {
        this.selectedTheme = selectedTheme;
    }

    /**
     * Checks if animations are enabled.
     *
     * @return true if animations are enabled, false otherwise
     */
    public boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    /**
     * Sets whether animations are enabled.
     *
     * @param animationsEnabled true to enable animations, false to disable
     */
    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }

    /**
     * Gets the optional accent color.
     *
     * @return the accent color, or null if not set
     */
    public String getAccentColor() {
        return accentColor;
    }

    /**
     * Sets the optional accent color.
     *
     * @param accentColor the accent color to set
     */
    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }
}

