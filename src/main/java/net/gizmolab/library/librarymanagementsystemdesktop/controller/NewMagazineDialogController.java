package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.picker.Cells;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.picker.PublisherPickerController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.MagazineDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.MagazineDraft;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** "Νέο περιοδικό": creates a local magazine, or lets the user pick the existing one on a duplicate (409). */
@Controller
@Scope("prototype")
public class NewMagazineDialogController extends BaseController {

    @Autowired private CatalogService catalog;

    @FXML private Label messageLabel;
    @FXML private TextField titleField;
    @FXML private TextField qualifierField;
    @FXML private TextField issnField;
    @FXML private TextField placeField;
    @FXML private TextField periodicityField;
    @FXML private PublisherPickerController publisherController;
    @FXML private Label errorLabel;
    @FXML private VBox duplicatesBox;
    @FXML private ListView<MagazineDTO> duplicatesList;
    @FXML private Button useExistingButton;
    @FXML private Button createButton;

    private MagazineDTO result;

    /**
     * @param lockedIssn   ISSN from a lookup that found nothing (shown, not editable), or null
     * @param initialTitle title the librarian searched for, or null
     * @param message      why the dialog opened (e.g. "Η Εθνική Βιβλιοθήκη δεν απάντησε…"), or null
     */
    public static Optional<MagazineDTO> open(FXMLLoaderFactory factory, Window owner,
                                             String lockedIssn, String initialTitle, String message) throws Exception {
        var loaded = factory.<Parent, NewMagazineDialogController>loadWithController("/fxml/new-magazine-dialog.fxml");
        NewMagazineDialogController controller = loaded.getController();
        controller.prefill(lockedIssn, initialTitle, message);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Νέο περιοδικό");
        Scene scene = new Scene(loaded.getRoot());
        StylesheetHelper.applyModernTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
        return Optional.ofNullable(controller.result);
    }

    /** "Τίτλος (προσδιορισμός) — ISSN … — Εκδότης": this dialog and the add-issue wizard. */
    public static String describe(MagazineDTO m) {
        List<String> parts = new ArrayList<>(List.of(m.getDisplayName()));
        if (m.getIssn() != null) parts.add("ISSN " + m.getIssn());
        if (m.getPublisher() != null) parts.add(m.getPublisher().getDisplayName());
        return String.join(" — ", parts);
    }

    @FXML
    private void initialize() {
        duplicatesList.setCellFactory(Cells.text(NewMagazineDialogController::describe));
        duplicatesList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> useExistingButton.setDisable(n == null));
    }

    private void prefill(String lockedIssn, String initialTitle, String message) {
        if (message != null) {
            messageLabel.setText(message);
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        }
        if (lockedIssn != null) {
            issnField.setText(lockedIssn);
            issnField.setEditable(false);
            issnField.setFocusTraversable(false);
        }
        if (initialTitle != null) titleField.setText(initialTitle.trim());
    }

    @FXML
    private void handleCreate() {
        MagazineDraft draft = new MagazineDraft();
        draft.setTitle(titleField.getText());
        draft.setQualifier(qualifierField.getText());
        draft.setIssn(issnField.getText());
        draft.setPlace(placeField.getText());
        draft.setPeriodicity(periodicityField.getText());
        PublisherDTO publisher = publisherController.getPublisher();
        draft.setPublisherId(publisher != null ? publisher.getId() : null);
        Map<String, String> errors = draft.validate();
        if (!errors.isEmpty()) {
            showError(String.join(" ", errors.values()));
            return;
        }
        createButton.setDisable(true);
        BackgroundTasks.run(() -> catalog.createMagazine(draft), created -> {
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

    private void showDuplicates(List<MagazineDTO> duplicates) {
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
        MagazineDTO chosen = duplicatesList.getSelectionModel().getSelectedItem();
        if (chosen != null) close(chosen);
    }

    @FXML
    private void handleCancel() {
        close(null);
    }

    private void close(MagazineDTO magazine) {
        result = magazine;
        ((Stage) createButton.getScene().getWindow()).close();
    }
}
