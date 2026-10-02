package net.gizmolab.library.librarymanagementsystemdesktop.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for applying modern theme CSS to JavaFX components.
 */
public final class StylesheetHelper {

    private StylesheetHelper() {}
    
    private static final Logger logger = LoggerFactory.getLogger(StylesheetHelper.class);
    private static final String MODERN_THEME_CSS = "/css/modern-theme.css";
    
    /**
     * Applies the modern theme CSS to a Scene.
     */
    public static void applyModernTheme(Scene scene) {
        if (scene != null) {
            try {
                String stylesheet = StylesheetHelper.class.getResource(MODERN_THEME_CSS).toExternalForm();
                if (!scene.getStylesheets().contains(stylesheet)) {
                    scene.getStylesheets().add(stylesheet);
                    logger.info("Applied modern CSS to Scene: {}", stylesheet);
                }
            } catch (Exception e) {
                logger.error("Failed to apply modern CSS to Scene", e);
            }
        }
    }
    
    /**
     * Applies the modern theme CSS to a Dialog.
     */
    public static void applyModernTheme(Dialog<?> dialog) {
        if (dialog != null && dialog.getDialogPane() != null) {
            try {
                String stylesheet = StylesheetHelper.class.getResource(MODERN_THEME_CSS).toExternalForm();
                if (!dialog.getDialogPane().getStylesheets().contains(stylesheet)) {
                    dialog.getDialogPane().getStylesheets().add(stylesheet);
                    logger.info("Applied modern CSS to Dialog: {}", stylesheet);
                }
            } catch (Exception e) {
                logger.error("Failed to apply modern CSS to Dialog", e);
            }
        }
    }
    
    /**
     * Applies the modern theme CSS to a Stage.
     */
    public static void applyModernTheme(Stage stage) {
        if (stage != null && stage.getScene() != null) {
            applyModernTheme(stage.getScene());
        }
    }
    
    /**
     * Applies the modern theme CSS to a Parent node.
     */
    public static void applyModernTheme(Parent parent) {
        if (parent != null) {
            try {
                String stylesheet = StylesheetHelper.class.getResource(MODERN_THEME_CSS).toExternalForm();
                if (!parent.getStylesheets().contains(stylesheet)) {
                    parent.getStylesheets().add(stylesheet);
                    logger.info("Applied modern CSS to Parent: {}", stylesheet);
                }
            } catch (Exception e) {
                logger.error("Failed to apply modern CSS to Parent", e);
            }
        }
    }
}
