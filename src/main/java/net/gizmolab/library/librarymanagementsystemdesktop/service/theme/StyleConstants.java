package net.gizmolab.library.librarymanagementsystemdesktop.service.theme;

import javafx.util.Duration;

/**
 * Design token constants for consistent styling across the application.
 * Provides color palette, spacing scale, typography, border radius, and animation durations.
 */
public class StyleConstants {

    // Color Palette
    public static final String PRIMARY_COLOR = "#2563eb";
    public static final String SECONDARY_COLOR = "#64748b";
    public static final String ACCENT_COLOR = "#8b5cf6";
    public static final String SUCCESS_COLOR = "#10b981";
    public static final String WARNING_COLOR = "#f59e0b";
    public static final String ERROR_COLOR = "#ef4444";

    // Spacing (8px grid system)
    public static final double SPACING_XS = 4;
    public static final double SPACING_SM = 8;
    public static final double SPACING_MD = 16;
    public static final double SPACING_LG = 24;
    public static final double SPACING_XL = 32;

    // Border Radius
    public static final double RADIUS_SM = 4;
    public static final double RADIUS_MD = 8;
    public static final double RADIUS_LG = 12;

    // Typography
    public static final String FONT_FAMILY = "Inter, 'Segoe UI', system-ui, sans-serif";
    public static final double FONT_SIZE_SM = 12;
    public static final double FONT_SIZE_BASE = 14;
    public static final double FONT_SIZE_LG = 16;
    public static final double FONT_SIZE_XL = 20;
    public static final double FONT_SIZE_2XL = 24;

    // Animation Durations
    public static final Duration ANIMATION_FAST = Duration.millis(150);
    public static final Duration ANIMATION_NORMAL = Duration.millis(250);
    public static final Duration ANIMATION_SLOW = Duration.millis(300);

    private StyleConstants() {
        // Prevent instantiation
    }
}
