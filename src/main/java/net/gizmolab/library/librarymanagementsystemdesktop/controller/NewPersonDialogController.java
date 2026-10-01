package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PersonDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.draft.PersonDraft;
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

/** "Νέο πρόσωπο": creates a local person, or lets the user pick the existing one on a duplicate (409). */
@Controller
@Scope("prototype")
public class NewPersonDialogController extends BaseController {

    @Autowired private CatalogService catalog;

    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField qualifierField;
    @FXML private TextField bornYearField;
    @FXML private TextField deathYearField;
    @FXML private Label errorLabel;
    @FXML private VBox duplicatesBox;
    @FXML private ListView<PersonDTO> duplicatesList;
    @FXML private Button useExistingButton;
    @FXML private Button createButton;

    private PersonDTO result;

    public static Optional<PersonDTO> open(FXMLLoaderFactory factory, Window owner, String initialName) throws Exception {
        var loaded = factory.<Parent, NewPersonDialogController>loadWithController("/fxml/new-person-dialog.fxml");
        NewPersonDialogController controller = loaded.getController();
        controller.prefill(initialName);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Νέο πρόσωπο");
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
            protected void updateItem(PersonDTO p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : describe(p));
            }
        });
        duplicatesList.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> useExistingButton.setDisable(n == null));
    }

    /** Search text of the picker: "Νίκη Λοϊζίδη" → first name "Νίκη", last name "Λοϊζίδη". */
    private void prefill(String name) {
        if (name == null || name.isBlank()) return;
        String[] parts = name.trim().split("\\s+", 2);
        firstnameField.setText(parts[0]);
        if (parts.length > 1) lastnameField.setText(parts[1]);
    }

    /** "Name (1950–) — Biblionet": used by this dialog and by the contributor picker. */
    public static String describe(PersonDTO p) {
        String years = p.getLifespan().isEmpty() ? "" : " (" + p.getLifespan() + ")";
        return p.getDisplayName() + years + " — " + (p.isFromBiblionet() ? "Biblionet" : "τοπική εγγραφή");
    }

    @FXML
    private void handleCreate() {
        PersonDraft draft = new PersonDraft();
        draft.setFirstname(firstnameField.getText());
        draft.setLastname(lastnameField.getText());
        draft.setQualifier(qualifierField.getText());
        draft.setBornYear(bornYearField.getText());
        draft.setDeathYear(deathYearField.getText());
        Map<String, String> errors = draft.validate();
        if (!errors.isEmpty()) {
            showError(String.join(" ", errors.values()));
            return;
        }
        createButton.setDisable(true);
        BackgroundTasks.run(() -> catalog.createPerson(draft), created -> {
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

    private void showDuplicates(List<PersonDTO> duplicates) {
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
        PersonDTO chosen = duplicatesList.getSelectionModel().getSelectedItem();
        if (chosen != null) close(chosen);
    }

    @FXML
    private void handleCancel() {
        close(null);
    }

    private void close(PersonDTO person) {
        result = person;
        ((Stage) createButton.getScene().getWindow()).close();
    }
}
