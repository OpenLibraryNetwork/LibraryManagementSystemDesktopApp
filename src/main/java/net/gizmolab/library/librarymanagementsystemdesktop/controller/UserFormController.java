package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.UserDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.regex.Pattern;

import javafx.scene.layout.HBox;
import java.util.Map;
import java.util.LinkedHashMap;

@Component
public class UserFormController extends BaseController {

    @Autowired
    private IUserService userService;

    @Autowired
    private I18nManager i18nManager;

    // FXML Components
    @FXML private Label dialogTitleLabel;
    @FXML private TextField firstNameField;
    @FXML private Label firstNameErrorLabel;
    @FXML private TextField lastNameField;
    @FXML private Label lastNameErrorLabel;
    @FXML private TextField emailField;
    @FXML private Label emailErrorLabel;
    @FXML private TextField phoneField;
    @FXML private Label phoneErrorLabel;
    @FXML private HBox errorContainer;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private final Map<Label, String> activeErrors = new LinkedHashMap<>();

    private Stage dialogStage;
    private UserDTO user;
    private boolean isEditMode = false;
    private Consumer<UserDTO> onSaveCallback;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @FXML
    private void initialize() {
        setupValidation();
        Platform.runLater(() -> firstNameField.requestFocus());
    }

    private void setupValidation() {
        // Add listeners for real-time validation
        firstNameField.textProperty().addListener((obs, oldVal, newVal) -> validateFirstName());
        lastNameField.textProperty().addListener((obs, oldVal, newVal) -> validateLastName());
        emailField.textProperty().addListener((obs, oldVal, newVal) -> validateEmail());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> validatePhone());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setUser(UserDTO user) {
        this.user = user;
        this.isEditMode = user != null;

        if (dialogTitleLabel != null) {
            if (isEditMode) {
                dialogTitleLabel.setText(i18nManager.getMessage("user.editUser"));
            } else {
                dialogTitleLabel.setText(i18nManager.getMessage("user.addUser"));
            }
        }
        if (isEditMode) {
            populateFields();
        }
    }

    public void setOnSaveCallback(Consumer<UserDTO> callback) {
        this.onSaveCallback = callback;
    }

    private void populateFields() {
        if (user != null) {
            firstNameField.setText(user.getFirstname());
            lastNameField.setText(user.getLastname());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhone());
        }
    }

    @FXML
    private void handleSave() {
        if (validateForm()) {
            saveUser();
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean validateForm() {
        boolean isValid = true;

        isValid &= validateFirstName();
        isValid &= validateLastName();
        isValid &= validateEmail();
        isValid &= validatePhone();

        return isValid;
    }

    private boolean validateFirstName() {
        String firstName = firstNameField.getText();
        if (firstName == null || firstName.trim().isEmpty()) {
            showFieldError(firstNameErrorLabel, i18nManager.getMessage("validation.required", i18nManager.getMessage("field.firstName")));
            return false;
        }
        hideFieldError(firstNameErrorLabel);
        return true;
    }

    private boolean validateLastName() {
        String lastName = lastNameField.getText();
        // Last name is optional in the model, so we don't require it
        hideFieldError(lastNameErrorLabel);
        return true;
    }

    private boolean validateEmail() {
        String email = emailField.getText();
        if (email != null && !email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showFieldError(emailErrorLabel, i18nManager.getMessage("validation.invalid.email", i18nManager.getMessage("user.email")));
                return false;
            }
        }
        hideFieldError(emailErrorLabel);
        return true;
    }

    private boolean validatePhone() {
        String phone = phoneField.getText();
        if (phone == null || phone.trim().isEmpty()) {
            showFieldError(phoneErrorLabel, i18nManager.getMessage("validation.required", i18nManager.getMessage("field.phone")));
            return false;
        }
        
        // Basic phone validation - remove spaces and check if it contains only digits, +, -, (, )
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)\\+]", "");
        if (!cleanPhone.matches("\\d+")) {
            showFieldError(phoneErrorLabel, i18nManager.getMessage("validation.invalid.phone", i18nManager.getMessage("field.phone")));
            return false;
        }
        
        hideFieldError(phoneErrorLabel);
        return true;
    }

    private void requestStageResize() {
        if (dialogStage != null) {
            Platform.runLater(() -> dialogStage.sizeToScene());
        }
    }

    private void updateWarningBox() {
        if (activeErrors.isEmpty()) {
            errorContainer.setVisible(false);
            errorContainer.setManaged(false);
        } else {
            String firstError = activeErrors.values().iterator().next();
            errorLabel.setText(firstError);
            errorContainer.setVisible(true);
            errorContainer.setManaged(true);
        }
        requestStageResize();
    }

    private void showFieldError(Label errorLabel, String message) {
        activeErrors.put(errorLabel, message);
        updateWarningBox();
    }

    private void hideFieldError(Label errorLabel) {
        activeErrors.remove(errorLabel);
        updateWarningBox();
    }

    private void saveUser() {
        UserDTO userToSave = createUserFromForm();

        Task<UserDTO> saveTask = new Task<UserDTO>() {
            @Override
            protected UserDTO call() throws Exception {
                User userEntity = convertToEntity(userToSave);
                User savedUser;
                
                if (isEditMode) {
                    savedUser = userService.updateUser(user.getUserId(), userEntity);
                } else {
                    savedUser = userService.createUser(userEntity);
                }
                
                return convertToDTO(savedUser);
            }
        };

        saveTask.setOnSucceeded(e -> {
            UserDTO savedUser = saveTask.getValue();
            if (onSaveCallback != null) {
                onSaveCallback.accept(savedUser);
            }
            Platform.runLater(() -> dialogStage.close());
        });

        saveTask.setOnFailed(e -> {
            Throwable exception = saveTask.getException();
            Platform.runLater(() -> {
                showError(i18nManager.getMessage("error.title"), 
                         i18nManager.getMessage("error.operation.failed", 
                                              isEditMode ? i18nManager.getMessage("user.operation.update") : i18nManager.getMessage("user.operation.create"), 
                                              exception.getMessage()));
            });
        });

        new Thread(saveTask).start();
    }

    private UserDTO createUserFromForm() {
        UserDTO userDTO = new UserDTO();
        
        if (isEditMode && user != null) {
            userDTO.setUserId(user.getUserId());
        }
        
        userDTO.setFirstname(firstNameField.getText().trim());
        userDTO.setLastname(lastNameField.getText().trim());
        userDTO.setEmail(emailField.getText().trim());
        userDTO.setPhone(phoneField.getText().trim());
        
        return userDTO;
    }

    private User convertToEntity(UserDTO userDTO) {
        User user = new User();
        user.setUserId(userDTO.getUserId());
        user.setFirstname(userDTO.getFirstname());
        user.setLastname(userDTO.getLastname());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        return user;
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
            user.getUserId(),
            user.getFirstname(),
            user.getLastname(),
            user.getEmail(),
            user.getPhone(),
            user.getActiveBorrowCount()
        );
    }

    public void updateUI() {
        Platform.runLater(() -> {
            if (dialogTitleLabel != null) {
                dialogTitleLabel.setText(isEditMode ? 
                    i18nManager.getMessage("user.editUser") : 
                    i18nManager.getMessage("user.addUser"));
            }
            if (cancelButton != null) {
                cancelButton.setText(i18nManager.getMessage("common.cancel"));
            }
            if (saveButton != null) {
                saveButton.setText(i18nManager.getMessage("common.save"));
            }
        });
    }
}