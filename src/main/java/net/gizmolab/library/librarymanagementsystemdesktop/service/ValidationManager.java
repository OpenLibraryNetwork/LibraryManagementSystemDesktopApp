package net.gizmolab.library.librarymanagementsystemdesktop.service;

import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ValidationManager provides comprehensive client-side validation for JavaFX forms.
 * It handles field validation, error display, and validation state management.
 */
@Component
public class ValidationManager {

    @Autowired
    private I18nManager i18nManager;

    // Common validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-()]{10,}$"
    );

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

    }

    /**
     * Field validator interface for custom validation logic
     */
    @FunctionalInterface
    public interface FieldValidator {
        ValidationResult validate(String value);
    }

    /**
     * Validates that a text field is not empty.
     */
    public ValidationResult validateRequired(TextField field, String fieldNameKey) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.required", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a ComboBox has a selection.
     */
    public ValidationResult validateRequired(ComboBox<?> comboBox, String fieldNameKey) {
        if (comboBox.getValue() == null) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.required", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a DatePicker has a date selected.
     */
    public ValidationResult validateRequired(DatePicker datePicker, String fieldNameKey) {
        if (datePicker.getValue() == null) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.required", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a text field contains a valid email address.
     */
    public ValidationResult validateEmail(TextField field, String fieldNameKey) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return new ValidationResult(true, null); // Empty is valid, use validateRequired separately
        }
        
        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.invalid.email", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a text field contains a valid phone number.
     */
    public ValidationResult validatePhone(TextField field, String fieldNameKey) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return new ValidationResult(true, null); // Empty is valid, use validateRequired separately
        }
        
        if (!PHONE_PATTERN.matcher(value.trim()).matches()) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.invalid.phone", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Displays validation error on a field with an error label.
     */
    public void showFieldError(Label errorLabel, String errorMessage) {
        if (errorLabel != null) {
            errorLabel.setText(errorMessage);
            errorLabel.setTextFill(Color.RED);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

}