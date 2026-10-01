package net.gizmolab.library.librarymanagementsystemdesktop.controller.picker;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.NewPersonDialogController;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.ContributorRoleDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PersonDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.CatalogService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.*;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Pick a role, search persons of the whole network, click (or Enter) to add; order with ▲▼, remove with ✕.
 * "Νέο πρόσωπο" creates a local person (or reuses an existing one on a duplicate).
 */
@Controller
@Scope("prototype")
public class ContributorPickerController extends BaseController {

    @Autowired private CatalogService catalog;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TextField searchField;
    @FXML private ComboBox<ContributorRoleDTO> roleCombo;
    @FXML private ListView<PersonDTO> suggestionsList;
    @FXML private Label statusLabel;
    @FXML private ListView<ContributorDTO> selectedList;

    private ContributorSelection selection = new ContributorSelection();
    private final LatestRequestGate gate = new LatestRequestGate();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    private boolean settingText;

    @FXML
    private void initialize() {
        suggestionsList.setCellFactory(Cells.text(NewPersonDialogController::describe));
        selectedList.setCellFactory(Cells.text(ContributorSelection::label));
        showSuggestions(false);

        searchField.textProperty().addListener((obs, old, text) -> {
            if (settingText) return;
            debounce.setOnFinished(e -> search(text));
            debounce.playFromStart();
        });
        // Click or Enter adds the person at once (arrow keys only move through the suggestions)
        suggestionsList.setOnMouseClicked(e -> add(suggestionsList.getSelectionModel().getSelectedItem()));
        suggestionsList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) add(suggestionsList.getSelectionModel().getSelectedItem());
        });

        BackgroundTasks.run(catalog::getRoles, roles -> {
            roleCombo.getItems().setAll(roles);
            roles.stream().filter(ContributorRoleDTO::isAuthor).findFirst()
                    .or(() -> roles.stream().findFirst())
                    .ifPresent(roleCombo::setValue);
        }, error -> status(UserMessages.describe(error)));
    }

    private void search(String text) {
        if (!SearchText.isSearchable(text)) {
            gate.next(); // an answer for the previous text must not reopen the suggestions
            showSuggestions(false);
            status("");
            return;
        }
        long token = gate.next();
        BackgroundTasks.run(() -> catalog.searchPersons(text), people -> {
            if (!gate.isLatest(token)) return; // an older answer arrived late
            suggestionsList.getItems().setAll(people);
            showSuggestions(!people.isEmpty());
            status(people.isEmpty() ? "Δεν βρέθηκε πρόσωπο. Χρησιμοποιήστε «Νέο πρόσωπο»." : addHint());
        }, error -> {
            if (gate.isLatest(token)) status(UserMessages.describe(error));
        });
    }

    private String addHint() {
        ContributorRoleDTO role = roleCombo.getValue();
        return role == null ? "" : "Κάντε κλικ σε ένα όνομα για να προστεθεί ως «" + role.getName() + "».";
    }

    /** Adds the person with the role shown next to the search field, then clears the search. */
    private void add(PersonDTO person) {
        if (person == null) return;
        if (roleCombo.getValue() == null) {
            status("Επιλέξτε ρόλο.");
            return;
        }
        if (!selection.add(person, roleCombo.getValue())) {
            status("Ο συντελεστής υπάρχει ήδη με αυτόν τον ρόλο.");
            return;
        }
        refreshSelected(selection.items().size() - 1);
        debounce.stop();
        gate.next(); // a search still in flight must not reopen the suggestions
        settingText = true;
        searchField.clear();
        settingText = false;
        showSuggestions(false);
        status("");
    }

    @FXML
    private void handleNewPerson() {
        try {
            NewPersonDialogController.open(fxmlLoaderFactory, searchField.getScene().getWindow(), searchField.getText())
                    .ifPresent(this::add);
        } catch (Exception e) {
            handleException("Νέο πρόσωπο", e);
        }
    }

    @FXML
    private void handleUp() {
        int i = selectedIndex();
        selection.moveUp(i);
        refreshSelected(Math.max(i - 1, 0));
    }

    @FXML
    private void handleDown() {
        int i = selectedIndex();
        selection.moveDown(i);
        refreshSelected(Math.min(i + 1, selection.items().size() - 1));
    }

    @FXML
    private void handleRemove() {
        int i = selectedIndex();
        selection.remove(i);
        refreshSelected(Math.min(i, selection.items().size() - 1));
    }

    /** No contributors and an empty search (the form is reused for another publication). */
    public void reset() {
        selection = new ContributorSelection();
        refreshSelected(-1);
        debounce.stop();
        gate.next();
        settingText = true;
        searchField.clear();
        settingText = false;
        showSuggestions(false);
        status("");
    }

    public List<ContributorDTO> getContributors() {
        return selection.items();
    }

    private int selectedIndex() {
        return selectedList.getSelectionModel().getSelectedIndex();
    }

    private void refreshSelected(int indexToSelect) {
        selectedList.getItems().setAll(selection.items());
        if (indexToSelect >= 0 && indexToSelect < selection.items().size()) {
            selectedList.getSelectionModel().select(indexToSelect);
        }
    }

    private void showSuggestions(boolean show) {
        suggestionsList.setVisible(show);
        suggestionsList.setManaged(show);
    }

    private void status(String text) {
        statusLabel.setText(text);
    }
}
