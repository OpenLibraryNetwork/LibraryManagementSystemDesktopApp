package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * AlertManager provides centralized alert and dialog management for the JavaFX application.
 * It ensures consistent styling, behavior, and internationalization across all dialogs.
 */
@Component
public class AlertManager {

    private static final Logger logger = LoggerFactory.getLogger(AlertManager.class);

    @Autowired
    private I18nManager i18nManager;

    /**
     * Shows an error alert with localized title and message.
     */
    public void showError(String messageKey, Object... args) {
        String title = i18nManager.getMessage("error.title");
        String message = i18nManager.getMessage(messageKey, args);
        showErrorDialog(title, message);
    }

    /**
     * Shows an error alert with custom title and message.
     */
    public void showError(String title, String message) {
        showErrorDialog(i18nManager.getMessage(title), i18nManager.getMessage(message));
    }

    /**
     * Shows a warning alert with localized title and message.
     */
    public void showWarning(String messageKey, Object... args) {
        String title = i18nManager.getMessage("warning.title");
        String message = i18nManager.getMessage(messageKey, args);
        showWarningDialog(title, message);
    }

    /**
     * Shows a warning alert with custom title and message.
     */
    public void showWarning(String title, String message) {
        showWarningDialog(i18nManager.getMessage(title), i18nManager.getMessage(message));
    }

    /**
     * Shows an information alert with localized title and message.
     */
    public void showInfo(String messageKey, Object... args) {
        String title = i18nManager.getMessage("info.title");
        String message = i18nManager.getMessage(messageKey, args);
        showInfoDialog(title, message);
    }

    /**
     * Shows an information alert with custom title and message.
     */
    public void showInfo(String title, String message) {
        showInfoDialog(i18nManager.getMessage(title), i18nManager.getMessage(message));
    }

    /**
     * Shows a success alert with localized message.
     */
    public void showSuccess(String messageKey, Object... args) {
        String title = i18nManager.getMessage("success.title");
        String message = i18nManager.getMessage(messageKey, args);
        showInfoDialog(title, message);
    }

    /**
     * Shows a confirmation dialog with localized message.
     * Returns true if user confirms (OK button), false otherwise.
     */
    public boolean showConfirmation(String messageKey, Object... args) {
        String title = i18nManager.getMessage("confirmation.title");
        String message = i18nManager.getMessage(messageKey, args);
        return showConfirmationDialog(title, message);
    }

    /**
     * Shows a confirmation dialog with custom title and message.
     * Returns true if user confirms (OK button), false otherwise.
     */
    public boolean showConfirmation(String title, String message) {
        return showConfirmationDialog(i18nManager.getMessage(title), i18nManager.getMessage(message));
    }

    /**
     * Shows a delete confirmation dialog with localized message.
     * Returns true if user confirms deletion, false otherwise.
     */
    public boolean showDeleteConfirmation(String entityType, String entityName) {
        String title = i18nManager.getMessage("confirmation.delete.title");
        String message = i18nManager.getMessage("confirmation.delete.message", entityType, entityName);
        
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message);
        
        // Customize buttons for delete confirmation
        ButtonType deleteButton = new ButtonType(i18nManager.getMessage("button.delete"));
        ButtonType cancelButton = new ButtonType(i18nManager.getMessage("button.cancel"));
        alert.getButtonTypes().setAll(deleteButton, cancelButton);
        
        // Add warning styling (in addition to modern styling from createAlert)
        alert.getDialogPane().getStyleClass().add("delete-confirmation-dialog");
        
        Optional<ButtonType> result = alert.showAndWait();
        boolean confirmed = result.isPresent() && result.get() == deleteButton;
        
        if (confirmed) {
            logger.debug("User confirmed deletion of {} '{}'", entityType, entityName);
        } else {
            logger.debug("User cancelled deletion of {} '{}'", entityType, entityName);
        }
        
        return confirmed;
    }

    // Private helper methods

    private void showErrorDialog(String title, String message) {
        runOnFXThread(() -> {
            Alert alert = createAlert(Alert.AlertType.ERROR, title, message);
            alert.showAndWait();
        });
    }

    private void showWarningDialog(String title, String message) {
        runOnFXThread(() -> {
            Alert alert = createAlert(Alert.AlertType.WARNING, title, message);
            alert.showAndWait();
        });
    }

    private void showInfoDialog(String title, String message) {
        runOnFXThread(() -> {
            Alert alert = createAlert(Alert.AlertType.INFORMATION, title, message);
            alert.showAndWait();
        });
    }

    private boolean showConfirmationDialog(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private Alert createAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Apply modern CSS theme
        StylesheetHelper.applyModernTheme(alert);
        
        // Apply modern styling
        alert.getDialogPane().getStyleClass().add("dialog-modern");
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        alert.getDialogPane().getStyleClass().add(type.name().toLowerCase() + "-alert");
        
        // Apply dialog open animation
        alert.setOnShowing(event -> AnimationHelper.dialogOpenAnimation(alert));
        
        return alert;
    }

    private void runOnFXThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}