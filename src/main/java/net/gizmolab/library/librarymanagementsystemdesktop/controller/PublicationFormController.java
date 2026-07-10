package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.function.Consumer;

@Controller
@Scope("prototype")
public class PublicationFormController extends BaseController {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private AuthService authService;

    // Type selection
    @FXML private ComboBox<String> typeComboBox;

    // Common fields
    @FXML private TextField titleField;
    @FXML private TextField pagesField;
    @FXML private TextField yearField;

    // Book-specific
    @FXML private VBox isbnSection;
    @FXML private TextField isbnField;
    @FXML private Button searchBiblionetButton;
    @FXML private VBox biblionetPreviewSection;
    @FXML private Label previewTitleLabel;
    @FXML private Label previewAuthorLabel;
    @FXML private Label previewPublisherLabel;

    // Periodical-specific
    @FXML private VBox periodicalSection;
    @FXML private ComboBox<String> magazineComboBox;
    @FXML private TextField issueNumberField;
    @FXML private TextField monthYearField;

    // Copies section
    @FXML private VBox copiesSection;
    @FXML private Spinner<Integer> copiesSpinner;
    @FXML private ComboBox<String> conditionComboBox;

    // Buttons
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private PublicationDTO editingPublication;
    private PublicationDTO biblionetResult;
    private Consumer<PublicationDTO> onSaveCallback;
    private List<Long> magazineIds = new ArrayList<>();

    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("Βιβλίο", "Μπροσούρα", "Περιοδικό");
        typeComboBox.setOnAction(e -> updateFieldVisibility());

        conditionComboBox.getItems().addAll("NEW", "GOOD", "FAIR", "POOR");
        conditionComboBox.setValue("NEW");

        copiesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));

        if (biblionetPreviewSection != null) {
            biblionetPreviewSection.setVisible(false);
        }

        loadMagazines();
    }

    private void updateFieldVisibility() {
        String type = typeComboBox.getValue();
        boolean isBook = "Βιβλίο".equals(type);
        boolean isPeriodical = "Περιοδικό".equals(type);

        if (isbnSection != null) {
            isbnSection.setVisible(isBook);
            isbnSection.setManaged(isBook);
        }
        if (biblionetPreviewSection != null) {
            biblionetPreviewSection.setVisible(false);
        }
        if (periodicalSection != null) {
            periodicalSection.setVisible(isPeriodical);
            periodicalSection.setManaged(isPeriodical);
        }

        // For books: fields filled by Biblionet
        titleField.setDisable(isBook && biblionetResult != null);
    }

    @FXML
    private void handleSearchBiblionet() {
        String isbn = isbnField.getText() != null ? isbnField.getText().trim() : "";
        if (isbn.isEmpty()) {
            alertManager.showWarning(i18nManager.getMessage("warning.title"), "Εισάγετε ISBN για αναζήτηση");
            return;
        }

        searchBiblionetButton.setDisable(true);

        Task<PublicationDTO> task = new Task<>() {
            @Override
            protected PublicationDTO call() throws Exception {
                JsonNode response = strapiApiClient.searchBiblionetByIsbn(isbn);
                if (response != null && response.has("data")) {
                    return DTOConverter.publicationFromJson(response.get("data"));
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            searchBiblionetButton.setDisable(false);
            biblionetResult = task.getValue();
            if (biblionetResult != null) {
                titleField.setText(biblionetResult.getTitle());
                pagesField.setText(biblionetResult.getPages() != null ?
                    String.valueOf(biblionetResult.getPages()) : "");
                yearField.setText(biblionetResult.getYearPublished() != null ?
                    String.valueOf(biblionetResult.getYearPublished()) : "");

                if (previewTitleLabel != null) previewTitleLabel.setText(biblionetResult.getTitle());
                if (previewAuthorLabel != null) previewAuthorLabel.setText(
                    biblionetResult.getAuthorNames() != null ? biblionetResult.getAuthorNames() : "—");
                if (previewPublisherLabel != null) previewPublisherLabel.setText(
                    biblionetResult.getPublisher() != null ? biblionetResult.getPublisher().getName() : "—");
                if (biblionetPreviewSection != null) biblionetPreviewSection.setVisible(true);

                titleField.setDisable(true);
                pagesField.setDisable(true);
                yearField.setDisable(true);

                alertManager.showInfo(i18nManager.getMessage("info.title"),
                    "Βιβλίο βρέθηκε! Προσθέστε αντίτυπα.");
            } else {
                alertManager.showWarning(i18nManager.getMessage("warning.title"),
                    "Δεν βρέθηκε βιβλίο με ISBN: " + isbn);
            }
        });

        task.setOnFailed(e -> {
            searchBiblionetButton.setDisable(false);
            handleException("Biblionet search", (Exception) task.getException());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        String type = typeComboBox.getValue();
        saveButton.setDisable(true);

        Task<PublicationDTO> task = new Task<>() {
            @Override
            protected PublicationDTO call() throws Exception {
                PublicationDTO result;

                if ("Βιβλίο".equals(type) && biblionetResult != null) {
                    result = biblionetResult;
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("type", type);
                    data.put("title", titleField.getText().trim());

                    if (pagesField.getText() != null && !pagesField.getText().isEmpty()) {
                        data.put("pages", Integer.parseInt(pagesField.getText().trim()));
                    }
                    if (yearField.getText() != null && !yearField.getText().isEmpty()) {
                        data.put("yearPublished", Integer.parseInt(yearField.getText().trim()));
                    }

                    if ("Περιοδικό".equals(type)) {
                        data.put("issueNumber", Integer.parseInt(issueNumberField.getText().trim()));
                        if (monthYearField.getText() != null && !monthYearField.getText().isEmpty()) {
                            data.put("publicationMonthYear", monthYearField.getText().trim());
                        }
                        int magIdx = magazineComboBox.getSelectionModel().getSelectedIndex();
                        if (magIdx >= 0 && magIdx < magazineIds.size()) {
                            data.put("magazine", magazineIds.get(magIdx));
                        }
                    }

                    JsonNode response = strapiApiClient.createPublication(data);
                    result = DTOConverter.publicationFromJson(response);
                }

                // Create copies
                if (result != null && copiesSpinner.getValue() > 0) {
                    int numCopies = copiesSpinner.getValue();
                    String condition = conditionComboBox.getValue();
                    for (int i = 1; i <= numCopies; i++) {
                        strapiApiClient.createCopy(result.getId(), i, condition);
                    }
                }

                return result;
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
            handleException("Save publication", (Exception) task.getException());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean validateForm() {
        String type = typeComboBox.getValue();
        if (type == null) {
            alertManager.showWarning(i18nManager.getMessage("warning.title"), "Επιλέξτε τύπο");
            return false;
        }
        if ("Βιβλίο".equals(type) && biblionetResult == null) {
            alertManager.showWarning(i18nManager.getMessage("warning.title"), "Αναζητήστε πρώτα με ISBN");
            return false;
        }
        if (!"Βιβλίο".equals(type)) {
            if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
                alertManager.showWarning(i18nManager.getMessage("warning.title"), "Ο τίτλος είναι υποχρεωτικός");
                return false;
            }
        }
        if ("Περιοδικό".equals(type)) {
            if (issueNumberField.getText() == null || issueNumberField.getText().trim().isEmpty()) {
                alertManager.showWarning(i18nManager.getMessage("warning.title"), "Ο αριθμός τεύχους είναι υποχρεωτικός");
                return false;
            }
        }
        return true;
    }

    private void loadMagazines() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                JsonNode response = strapiApiClient.getAllMagazines();
                var magazines = DTOConverter.magazinesFromJson(response);
                javafx.application.Platform.runLater(() -> {
                    magazineComboBox.getItems().clear();
                    magazineIds.clear();
                    for (var mag : magazines) {
                        magazineComboBox.getItems().add(mag.getTitle());
                        magazineIds.add(mag.getId());
                    }
                });
                return null;
            }
        };
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

    // Setters
    public void setPublication(PublicationDTO pub) {
        this.editingPublication = pub;
        if (pub != null) {
            typeComboBox.setValue(pub.getType());
            titleField.setText(pub.getTitle());
            if (pub.getPages() != null) pagesField.setText(String.valueOf(pub.getPages()));
            if (pub.getYearPublished() != null) yearField.setText(String.valueOf(pub.getYearPublished()));
            updateFieldVisibility();
        }
    }

    public void setDefaultType(String type) {
        typeComboBox.setValue(type);
        updateFieldVisibility();
    }

    public void setOnSaveCallback(Consumer<PublicationDTO> callback) {
        this.onSaveCallback = callback;
    }
}
