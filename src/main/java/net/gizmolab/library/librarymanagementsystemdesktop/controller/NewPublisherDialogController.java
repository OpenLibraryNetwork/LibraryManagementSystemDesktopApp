package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PublisherDraft;
import net.gizmolab.library.librarymanagementsystemdesktop.service.CatalogService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** "Νέος εκδότης": creates a local publisher, or lets the user pick the existing one on a duplicate (409). */
@Controller
@Scope("prototype")
public class NewPublisherDialogController extends BaseController {

    @Autowired private CatalogService catalog;

    @FXML private TextField nameField;
    @FXML private TextField qualifierField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField websiteField;
    @FXML private Label errorLabel;
    @FXML private VBox duplicatesBox;
    @FXML private ListView<PublisherDTO> duplicatesList;
    @FXML private Button useExistingButton;
    @FXML private Button createButton;

    private PublisherDTO result;

    public static Optional<PublisherDTO> open(FXMLLoaderFactory factory, Window owner, String initialName) throws Exception {
        var loaded = factory.<Parent, NewPublisherDialogController>loadWithController("/fxml/new-publisher-dialog.fxml");
        NewPublisherDialogController controller = loaded.getController();
        if (initialName != null) controller.nameField.setText(initialName.trim());
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Νέος εκδότης");
        Scene scene = new Scene(loaded.getRoot());
        StylesheetHelper.applyModernTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
        return Optional.ofNullable(controller.result);
    }

    @FXML
    private void initialize() {
        duplicatesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PublisherDTO p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null
                        : p.getDisplayName() + " — " + (p.isFromBiblionet() ? "Biblionet" : "τοπική εγγραφή"));
            }
        });
        duplicatesList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> useExistingButton.setDisable(n == null));
    }

    @FXML
    private void handleCreate() {
        PublisherDraft draft = new PublisherDraft();
        draft.setName(nameField.getText());
        draft.setQualifier(qualifierField.getText());
        draft.setAddress(addressField.getText());
        draft.setPhone(phoneField.getText());
        draft.setEmail(emailField.getText());
        draft.setWebsite(websiteField.getText());
        Map<String, String> errors = draft.validate();
        if (!errors.isEmpty()) {
            showError(String.join(" ", errors.values()));
            return;
        }
        createButton.setDisable(true);
        BackgroundTasks.run(() -> catalog.createPublisher(draft), created -> {
            createButton.setDisable(false);
            if (created.isDuplicate()) {
                showDuplicates(created.duplicates());
            } else {
                close(created.created());
            }
        }, error -> {
            createButton.setDisable(false);
            showError(UserMessages.describe(error));
        });
    }

    private void showDuplicates(List<PublisherDTO> duplicates) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        duplicatesList.getItems().setAll(duplicates);
        if (!duplicates.isEmpty()) duplicatesList.getSelectionModel().selectFirst();
        duplicatesBox.setVisible(true);
        duplicatesBox.setManaged(true);
        duplicatesBox.getScene().getWindow().sizeToScene();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML
    private void handleUseExisting() {
        PublisherDTO chosen = duplicatesList.getSelectionModel().getSelectedItem();
        if (chosen != null) close(chosen);
    }

    @FXML
    private void handleCancel() {
        close(null);
    }

    private void close(PublisherDTO publisher) {
        result = publisher;
        ((Stage) createButton.getScene().getWindow()).close();
    }
}
