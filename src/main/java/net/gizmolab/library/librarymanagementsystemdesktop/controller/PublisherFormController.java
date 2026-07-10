package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
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
public class PublisherFormController extends BaseController {

    @Autowired private StrapiApiClient strapiApiClient;

    @FXML private TextField nameField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private PublisherDTO editingPublisher;
    private Consumer<PublisherDTO> onSaveCallback;

    public void setPublisher(PublisherDTO publisher) {
        this.editingPublisher = publisher;
        if (publisher != null) {
            nameField.setText(publisher.getName());
        }
    }

    public void setOnSaveCallback(Consumer<PublisherDTO> callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        if (name.isEmpty()) {
            alertManager.showWarning(i18nManager.getMessage("warning.title"),
                i18nManager.getMessage("publisher.name.required", "Name is required"));
            return;
        }

        saveButton.setDisable(true);

        Task<PublisherDTO> task = new Task<>() {
            @Override
            protected PublisherDTO call() throws Exception {
                JsonNode response = strapiApiClient.createPublisher(name);
                return DTOConverter.publisherFromJson(response);
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
            handleException("Save publisher", (Exception) task.getException());
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
