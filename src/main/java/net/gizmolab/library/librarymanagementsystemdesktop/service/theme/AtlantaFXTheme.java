package net.gizmolab.library.librarymanagementsystemdesktop.service.theme;

/**
 * Simple theme enum for compatibility.
 * No longer uses AtlantaFX - just a placeholder for theme preferences.
 */
public enum AtlantaFXTheme {
    PRIMER_LIGHT("Primer Light", false),
    PRIMER_DARK("Primer Dark", true),
    NORD_LIGHT("Nord Light", false),
    NORD_DARK("Nord Dark", true);

    private final String displayName;
    private final boolean dark;

    AtlantaFXTheme(String displayName, boolean dark) {
        this.displayName = displayName;
        this.dark = dark;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDark() {
        return dark;
    }

    /**
     * Returns null - we don't use User Agent Stylesheets anymore.
     */
    public String getUserAgentStylesheet() {
        return null;
    }
}
