package net.gizmolab.library.librarymanagementsystemdesktop.controller.picker;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.NewPublisherDialogController;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.CatalogService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.LatestRequestGate;
import net.gizmolab.library.librarymanagementsystemdesktop.util.SearchText;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/** One publisher (or none) from the whole network; "Νέος εκδότης" creates a local one. */
@Controller
@Scope("prototype")
public class PublisherPickerController extends BaseController {

    @Autowired private CatalogService catalog;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TextField searchField;
    @FXML private ListView<PublisherDTO> suggestionsList;
    @FXML private Label selectedLabel;
    @FXML private Button clearButton;
    @FXML private Label statusLabel;

    private final LatestRequestGate gate = new LatestRequestGate();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    private PublisherDTO selected;

    @FXML
    private void initialize() {
        suggestionsList.setCellFactory(Cells.text(p ->
                p.getDisplayName() + " — " + (p.isFromBiblionet() ? "Biblionet" : "τοπική εγγραφή")));
        showSuggestions(false);
        searchField.textProperty().addListener((obs, old, text) -> {
            debounce.setOnFinished(e -> search(text));
            debounce.playFromStart();
        });
        // Click or Enter selects (arrow keys only move through the suggestions)
        suggestionsList.setOnMouseClicked(e -> select(suggestionsList.getSelectionModel().getSelectedItem()));
        suggestionsList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) select(suggestionsList.getSelectionModel().getSelectedItem());
        });
    }

    private void search(String text) {
        if (!SearchText.isSearchable(text)) {
            gate.next(); // an answer for the previous text must not reopen the suggestions
            showSuggestions(false);
            statusLabel.setText("");
            return;
        }
        long token = gate.next();
        BackgroundTasks.run(() -> catalog.searchPublishers(text), publishers -> {
            if (!gate.isLatest(token)) return;
            suggestionsList.getItems().setAll(publishers);
            showSuggestions(!publishers.isEmpty());
            statusLabel.setText(publishers.isEmpty() ? "Δεν βρέθηκε εκδότης. Χρησιμοποιήστε «Νέος εκδότης»." : "");
        }, error -> {
            if (gate.isLatest(token)) statusLabel.setText(UserMessages.describe(error));
        });
    }

    private void select(PublisherDTO publisher) {
        if (publisher == null) return;
        debounce.stop();
        gate.next(); // a search still in flight must not reopen the suggestions
        searchField.clear();
        selected = publisher;
        selectedLabel.setText(publisher.getDisplayName());
        clearButton.setDisable(false);
        showSuggestions(false);
        statusLabel.setText("");
    }

    @FXML
    private void handleNewPublisher() {
        try {
            NewPublisherDialogController.open(fxmlLoaderFactory, searchField.getScene().getWindow(), searchField.getText())
                    .ifPresent(this::select);
        } catch (Exception e) {
            handleException("Νέος εκδότης", e);
        }
    }

    @FXML
    private void handleClear() {
        selected = null;
        selectedLabel.setText("—");
        clearButton.setDisable(true);
    }

    /** Back to "no publisher" with an empty search (the form is reused for another publication). */
    public void reset() {
        handleClear();
        debounce.stop();
        gate.next();
        searchField.clear();
        showSuggestions(false);
        statusLabel.setText("");
    }

    public PublisherDTO getPublisher() {
        return selected;
    }

    private void showSuggestions(boolean show) {
        suggestionsList.setVisible(show);
        suggestionsList.setManaged(show);
    }
}
