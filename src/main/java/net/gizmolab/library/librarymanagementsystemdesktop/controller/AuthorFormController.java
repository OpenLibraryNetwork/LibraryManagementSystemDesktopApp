package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.AuthorDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.function.Consumer;

@Controller
@Scope("prototype")
public class AuthorFormController extends BaseController {

    @Autowired private StrapiApiClient strapiApiClient;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField nameField;          // Auto-computed display name
    @FXML private TextField biblionetIdField;   // Read-only
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private AuthorDTO editingAuthor;
    private Consumer<AuthorDTO> onSaveCallback;

    @FXML
    public void initialize() {
        // Auto-compute name from firstname + lastname
        firstNameField.textProperty().addListener((obs, o, n) -> updateNameField());
        lastNameField.textProperty().addListener((obs, o, n) -> updateNameField());

        if (nameField != null) {
            nameField.setEditable(false);
        }
        if (biblionetIdField != null) {
            biblionetIdField.setEditable(false);
        }
    }

    private void updateNameField() {
        String first = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String last = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        String computed = (first + " " + last).trim();
        if (nameField != null) {
            nameField.setText(computed);
        }
    }

    public void setAuthor(AuthorDTO author) {
        this.editingAuthor = author;
        if (author != null) {
            firstNameField.setText(author.getFirstname() != null ? author.getFirstname() : "");
            lastNameField.setText(author.getLastname() != null ? author.getLastname() : "");
            if (nameField != null) {
                nameField.setText(author.getDisplayName());
            }
            if (biblionetIdField != null && author.getBiblionetPersonId() != null) {
                biblionetIdField.setText(author.getBiblionetPersonId());
            }
        }
    }

    public void setOnSaveCallback(Consumer<AuthorDTO> callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        String firstname = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String lastname = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        String name = (firstname + " " + lastname).trim();

        if (name.isEmpty()) {
            alertManager.showWarning(i18nManager.getMessage("warning.title"),
                i18nManager.getMessage("author.name.required", "Name is required"));
            return;
        }

        saveButton.setDisable(true);

        Task<AuthorDTO> task = new Task<>() {
            @Override
            protected AuthorDTO call() throws Exception {
                JsonNode response = strapiApiClient.createAuthor(name, firstname, lastname);
                return DTOConverter.authorFromJson(response);
            }
        };

        task.setOnSucceeded(e -> {
            saveButton.setDisable(false);
            if (onSaveCallback != null && task.getValue() != null) {
                onSaveCallback.accept(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            saveButton.setDisable(false);
            handleException("Save author", (Exception) task.getException());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel() {
        if (cancelButton != null && cancelButton.getScene() != null) {
            cancelButton.getScene().getWindow().hide();
        }
    }
}
