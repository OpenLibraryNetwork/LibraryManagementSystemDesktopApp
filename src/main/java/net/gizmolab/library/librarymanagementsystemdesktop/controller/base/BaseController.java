package net.gizmolab.library.librarymanagementsystemdesktop.controller.base;

import net.gizmolab.library.librarymanagementsystemdesktop.service.AlertManager;
import net.gizmolab.library.librarymanagementsystemdesktop.service.GlobalExceptionHandler;
import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Base controller class that provides common functionality for all JavaFX controllers.
 * This includes error handling, confirmation dialogs, validation, and utility methods.
 */
public abstract class BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    protected ResourceBundle resources;
    
    protected java.util.function.Consumer<String> statusUpdateCallback;

    @Autowired
    protected AlertManager alertManager;

    @Autowired
    protected GlobalExceptionHandler exceptionHandler;

    @Autowired
    protected I18nManager i18nManager;

    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * Sets the resource bundle for internationalization.
     * This will be called by the FXML loader when the view is loaded.
     */
    public void setResources(ResourceBundle resources) {
        this.resources = resources;
    }
    
    /**
     * Sets the callback for status updates.
     * This allows child controllers to update the main status bar.
     */
    public void setStatusUpdateCallback(java.util.function.Consumer<String> callback) {
        this.statusUpdateCallback = callback;
    }
    
    /**
     * Shows an error dialog with the specified title and message.
     * Delegates to the AlertManager when it is set, otherwise falls back to a plain JavaFX dialog.
     */
    protected void showError(String title, String message) {
        if (alertManager != null) {
            alertManager.showError(title, message);
        } else {
            // Fallback for cases where AlertManager is not available
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                StylesheetHelper.applyModernTheme(alert);
                alert.getDialogPane().getStyleClass().add("dialog-modern");
                alert.showAndWait();
            });
        }
    }

    /**
     * Shows a warning dialog with the specified title and message.
     * Delegates to the AlertManager when it is set, otherwise falls back to a plain JavaFX dialog.
     */
    protected void showWarning(String title, String message) {
        if (alertManager != null) {
            alertManager.showWarning(title, message);
        } else {
            // Fallback for cases where AlertManager is not available
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                StylesheetHelper.applyModernTheme(alert);
                alert.getDialogPane().getStyleClass().add("dialog-modern");
                alert.showAndWait();
            });
        }
    }

    /**
     * Shows an information dialog with the specified title and message.
     * Delegates to the AlertManager when it is set, otherwise falls back to a plain JavaFX dialog.
     */
    protected void showInfo(String title, String message) {
        if (alertManager != null) {
            alertManager.showInfo(title, message);
        } else {
            // Fallback for cases where AlertManager is not available
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                StylesheetHelper.applyModernTheme(alert);
                alert.getDialogPane().getStyleClass().add("dialog-modern");
                alert.showAndWait();
            });
        }
    }

    /**
     * Shows a confirmation dialog and returns true if the user confirms.
     * Delegates to the AlertManager when it is set, otherwise falls back to a plain JavaFX dialog.
     */
    protected boolean showConfirmation(String title, String message) {
        if (alertManager != null) {
            return alertManager.showConfirmation(title, message);
        } else {
            // Fallback for cases where AlertManager is not available
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            StylesheetHelper.applyModernTheme(alert);
            alert.getDialogPane().getStyleClass().add("dialog-modern");
            
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }
    }

    /**
     * Shows a confirmation dialog with custom button text.
     * Delegates to the AlertManager when it is set, otherwise falls back to a plain JavaFX dialog.
     */
    protected boolean showConfirmation(String title, String message, String confirmText, String cancelText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        StylesheetHelper.applyModernTheme(alert);
        alert.getDialogPane().getStyleClass().add("dialog-modern");
        
        ButtonType confirmButton = new ButtonType(confirmText);
        ButtonType cancelButton = new ButtonType(cancelText);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirmButton;
    }

    /**
     * Shows a delete confirmation dialog using the AlertManager.
     */
    protected boolean showDeleteConfirmation(String entityType, String entityName) {
        if (alertManager != null) {
            return alertManager.showDeleteConfirmation(entityType, entityName);
        } else {
            // Fallback
            return showConfirmation("Confirm Delete", "Are you sure you want to delete " + entityType + " '" + entityName + "'?");
        }
    }

    /**
     * Gets a localized message from the resource bundle.
     * Returns the key if the resource bundle is not available or the key is not found.
     */
    protected String getLocalizedMessage(String key) {
        if (resources != null && resources.containsKey(key)) {
            return resources.getString(key);
        }
        return key; // Fallback to key if not found
    }

    /**
     * Gets a localized message with parameters.
     */
    protected String getLocalizedMessage(String key, Object... args) {
        String message = getLocalizedMessage(key);
        if (args.length > 0) {
            // The bundles use MessageFormat placeholders ("Σύνολο: {0} στοιχεία"), not String.format ones
            return java.text.MessageFormat.format(message, args);
        }
        return message;
    }

    /**
     * Logs the error and shows a short Greek message to the user.
     * The GlobalExceptionHandler only logs, so without this the user would see nothing.
     */
    protected void handleException(String operation, Exception e) {
        if (exceptionHandler != null) {
            exceptionHandler.handleException(operation, e);
        } else {
            logger.error("Error during {}: {}", operation, e.getMessage(), e);
        }
        String title = getLocalizedMessage("error.title");
        String message = UserMessages.describe(e);
        if (Platform.isFxApplicationThread()) {
            showError(title, message);
        } else {
            Platform.runLater(() -> showError(title, message));
        }
    }

    /**
     * Safely executes an operation with comprehensive exception handling.
     */
    protected void executeWithExceptionHandling(String operation, Runnable task) {
        if (exceptionHandler != null) {
            exceptionHandler.executeWithExceptionHandling(operation, task);
        } else {
            try {
                task.run();
            } catch (Exception e) {
                handleException(operation, e);
            }
        }
    }

    /**
     * Safely executes an operation with comprehensive exception handling and return value.
     */
    protected <T> T executeWithExceptionHandling(String operation, java.util.concurrent.Callable<T> task, T defaultValue) {
        if (exceptionHandler != null) {
            return exceptionHandler.executeWithExceptionHandling(operation, task, defaultValue);
        } else {
            try {
                return task.call();
            } catch (Exception e) {
                handleException(operation, e);
                return defaultValue;
            }
        }
    }

    /**
     * Safely executes a runnable on the JavaFX Application Thread.
     */
    protected void runOnFXThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    /**
     * Logs an info message with the controller class name.
     */
    protected void logInfo(String message, Object... args) {
        if (logger.isInfoEnabled()) logger.info("[{}] {}", this.getClass().getSimpleName(), String.format(message, args));
    }

    /**
     * Logs an error message with the controller class name.
     */
    protected void logError(String message, Throwable throwable) {
        logger.error("[{}] {}", this.getClass().getSimpleName(), message, throwable);
    }

    /**
     * Gets the Spring ApplicationContext for accessing beans.
     */
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}