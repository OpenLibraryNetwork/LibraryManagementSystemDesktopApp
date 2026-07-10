package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.MagazineDTO;
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

import java.util.List;
import java.util.function.Consumer;

@Controller
@Scope("prototype")
public class MagazineFormController extends BaseController {

    @Autowired private StrapiApiClient strapiApiClient;

    @FXML private TextField titleField;
    @FXML private TextField issnField;
    @FXML private ComboBox<PublisherDTO> publisherComboBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private MagazineDTO editingMagazine;
    private Consumer<MagazineDTO> onSaveCallback;

    @FXML
    public void initialize() {
        loadPublishers();
    }

    private void loadPublishers() {
        Task<List<PublisherDTO>> task = new Task<>() {
            @Override
            protected List<PublisherDTO> call() throws Exception {
                JsonNode response = strapiApiClient.getAllPublishers();
                return DTOConverter.publishersFromJson(response);
            }
        };

        task.setOnSucceeded(e -> {
            publisherComboBox.getItems().clear();
            publisherComboBox.getItems().addAll(task.getValue());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void setMagazine(MagazineDTO magazine) {
        this.editingMagazine = magazine;
        if (magazine != null) {
            titleField.setText(magazine.getTitle());
            issnField.setText(magazine.getIssn() != null ? magazine.getIssn() : "");
            if (magazine.getPublisher() != null) {
                // Select matching publisher in combo
                for (PublisherDTO pub : publisherComboBox.getItems()) {
                    if (pub.getId().equals(magazine.getPublisher().getId())) {
                        publisherComboBox.setValue(pub);
                        break;
                    }
                }
            }
        }
    }

    public void setOnSaveCallback(Consumer<MagazineDTO> callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText() != null ? titleField.getText().trim() : "";
        if (title.isEmpty()) {
            alertManager.showWarning(i18nManager.getMessage("warning.title"),
                i18nManager.getMessage("magazine.title.required", "Title is required"));
            return;
        }

        String issn = issnField.getText() != null ? issnField.getText().trim() : null;
        PublisherDTO selectedPublisher = publisherComboBox.getValue();
        Long publisherId = selectedPublisher != null ? selectedPublisher.getId() : null;

        saveButton.setDisable(true);

        Task<MagazineDTO> task = new Task<>() {
            @Override
            protected MagazineDTO call() throws Exception {
                JsonNode response = strapiApiClient.createMagazine(title, issn, publisherId);
                return DTOConverter.magazineFromJson(response);
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
            handleException("Save magazine", (Exception) task.getException());
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
