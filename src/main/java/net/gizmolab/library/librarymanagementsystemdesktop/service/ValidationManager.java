package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.validation.ISBNValidator;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

        public List<String> getErrors() {
            return errors;
        }

        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
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
     * Validates text length constraints.
     */
    public ValidationResult validateLength(TextField field, String fieldNameKey, int minLength, int maxLength) {
        String value = field.getText();
        if (value == null) value = "";
        
        List<String> errors = new ArrayList<>();
        String fieldName = i18nManager.getMessage(fieldNameKey);
        
        if (value.length() < minLength) {
            errors.add(i18nManager.getMessage("validation.min.length", fieldName, minLength));
        }
        
        if (value.length() > maxLength) {
            errors.add(i18nManager.getMessage("validation.max.length", fieldName, maxLength));
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
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
     * Validates that a text field contains a valid ISBN.
     */
    public ValidationResult validateISBN(TextField field, String fieldNameKey) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return new ValidationResult(true, null); // Empty is valid, use validateRequired separately
        }
        
        if (!ISBNValidator.isValid(value.trim())) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.invalid.isbn", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a text field contains a valid positive integer.
     */
    public ValidationResult validatePositiveInteger(TextField field, String fieldNameKey) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return new ValidationResult(true, null); // Empty is valid, use validateRequired separately
        }
        
        try {
            int intValue = Integer.parseInt(value.trim());
            if (intValue <= 0) {
                String fieldName = i18nManager.getMessage(fieldNameKey);
                String error = i18nManager.getMessage("validation.positive.number", fieldName);
                return new ValidationResult(false, List.of(error));
            }
        } catch (NumberFormatException e) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.invalid.number", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a text field contains a valid non-negative integer.
     */
    public ValidationResult validateNonNegativeInteger(TextField field, String fieldNameKey) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return new ValidationResult(true, null); // Empty is valid, use validateRequired separately
        }
        
        try {
            int intValue = Integer.parseInt(value.trim());
            if (intValue < 0) {
                String fieldName = i18nManager.getMessage(fieldNameKey);
                String error = i18nManager.getMessage("validation.non.negative.number", fieldName);
                return new ValidationResult(false, List.of(error));
            }
        } catch (NumberFormatException e) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.invalid.number", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a date is not in the future.
     */
    public ValidationResult validateDateNotFuture(DatePicker datePicker, String fieldNameKey) {
        LocalDate value = datePicker.getValue();
        if (value == null) {
            return new ValidationResult(true, null); // Empty is valid, use validateRequired separately
        }
        
        if (value.isAfter(LocalDate.now())) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.date.not.future", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validates that a date is not in the past.
     */
    public ValidationResult validateDateNotPast(DatePicker datePicker, String fieldNameKey) {
        LocalDate value = datePicker.getValue();
        if (value == null) {
            return new ValidationResult(true, null); // Empty is valid, use validateRequired separately
        }
        
        if (value.isBefore(LocalDate.now())) {
            String fieldName = i18nManager.getMessage(fieldNameKey);
            String error = i18nManager.getMessage("validation.date.not.past", fieldName);
            return new ValidationResult(false, List.of(error));
        }
        
        return new ValidationResult(true, null);
    }

    /**
     * Validates a field using a custom validator.
     */
    public ValidationResult validateCustom(TextField field, FieldValidator validator) {
        String value = field.getText();
        return validator.validate(value);
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

    /**
     * Clears validation error from a field.
     */
    public void clearFieldError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Adds visual validation styling to a field.
     */
    public void addValidationStyling(Control field, boolean isValid) {
        field.getStyleClass().removeAll("field-valid", "field-invalid");
        if (isValid) {
            field.getStyleClass().add("field-valid");
        } else {
            field.getStyleClass().add("field-invalid");
        }
    }

    /**
     * Removes validation styling from a field.
     */
    public void removeValidationStyling(Control field) {
        field.getStyleClass().removeAll("field-valid", "field-invalid");
    }

    /**
     * Validates multiple fields and returns combined result.
     */
    public ValidationResult validateFields(ValidationResult... results) {
        List<String> allErrors = new ArrayList<>();
        boolean allValid = true;
        
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                allValid = false;
                allErrors.addAll(result.getErrors());
            }
        }
        
        return new ValidationResult(allValid, allErrors);
    }

    /**
     * Sets up real-time validation for a text field.
     */
    public void setupRealTimeValidation(TextField field, Label errorLabel, String fieldNameKey, FieldValidator... validators) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            List<String> errors = new ArrayList<>();
            boolean isValid = true;
            
            for (FieldValidator validator : validators) {
                ValidationResult result = validator.validate(newValue);
                if (!result.isValid()) {
                    isValid = false;
                    errors.addAll(result.getErrors());
                }
            }
            
            if (isValid) {
                clearFieldError(errorLabel);
                addValidationStyling(field, true);
            } else {
                showFieldError(errorLabel, errors.get(0)); // Show first error
                addValidationStyling(field, false);
            }
        });
    }

    /**
     * Creates a required field validator.
     */
    public FieldValidator createRequiredValidator(String fieldNameKey) {
        return value -> {
            if (value == null || value.trim().isEmpty()) {
                String fieldName = i18nManager.getMessage(fieldNameKey);
                String error = i18nManager.getMessage("validation.required", fieldName);
                return new ValidationResult(false, List.of(error));
            }
            return new ValidationResult(true, null);
        };
    }

    /**
     * Creates a length validator.
     */
    public FieldValidator createLengthValidator(String fieldNameKey, int minLength, int maxLength) {
        return value -> {
            if (value == null) value = "";
            
            List<String> errors = new ArrayList<>();
            String fieldName = i18nManager.getMessage(fieldNameKey);
            
            if (value.length() < minLength) {
                errors.add(i18nManager.getMessage("validation.min.length", fieldName, minLength));
            }
            
            if (value.length() > maxLength) {
                errors.add(i18nManager.getMessage("validation.max.length", fieldName, maxLength));
            }
            
            return new ValidationResult(errors.isEmpty(), errors);
        };
    }
}