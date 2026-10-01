package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.SearchText;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/** "Προσθήκη μπροσούρας": search the network catalog → pick an existing brochure or catalogue a new one → copies. */
@Controller
@Scope("prototype")
public class AddBrochureWizardController extends AddPublicationWizardController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private TableView<PublicationDTO> resultsTable;
    @FXML private TableColumn<PublicationDTO, String> titleColumn;
    @FXML private TableColumn<PublicationDTO, String> authorsColumn;
    @FXML private TableColumn<PublicationDTO, String> publisherColumn;
    @FXML private TableColumn<PublicationDTO, String> yearColumn;
    @FXML private TableColumn<PublicationDTO, String> inLibraryColumn;
    @FXML private Label searchMessage;
    @FXML private Button selectButton;
    @FXML private Button newBrochureButton;

    public static void open(FXMLLoaderFactory factory, Window owner) throws Exception {
        openWizard(factory, owner, "/fxml/add-brochure-wizard.fxml", "Προσθήκη μπροσούρας");
    }

    @Override
    protected String duplicatesMessage() {
        return "Υπάρχει ήδη μπροσούρα με τα ίδια στοιχεία (τίτλος, εκδότης, έτος). Χρησιμοποιήστε την υπάρχουσα:";
    }

    @Override
    protected void initialize() {
        super.initialize();
        titleColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        authorsColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuthorNames()));
        publisherColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPublisher() != null ? d.getValue().getPublisher().getDisplayName() : ""));
        yearColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getYearPublished() != null ? String.valueOf(d.getValue().getYearPublished()) : ""));
        inLibraryColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTotalCopies() > 0 ? d.getValue().getTotalCopies() + " αντίτυπα" : "—"));
        resultsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, pub) -> selectButton.setDisable(pub == null));
        resultsTable.setRowFactory(table -> {
            TableRow<PublicationDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) handleSelect();
            });
            return row;
        });
        searchMessage.setText("Αναζητήστε με τίτλο. Αν η μπροσούρα δεν υπάρχει, επιλέξτε «Νέα μπροσούρα».");
        // "Νέα μπροσούρα" only after a search for exactly this title, so nobody catalogues without looking first
        searchField.textProperty().addListener((obs, old, text) -> newBrochureButton.setDisable(true));
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (!SearchText.isSearchable(query)) {
            resultsTable.getItems().clear();
            searchMessage.setText("Πληκτρολογήστε τουλάχιστον 2 χαρακτήρες.");
            return;
        }
        searchButton.setDisable(true);
        busyIndicator.setVisible(true);
        BackgroundTasks.run(() -> catalog.searchBrochures(query), brochures -> {
            searchButton.setDisable(false);
            busyIndicator.setVisible(false);
            resultsTable.getItems().setAll(brochures);
            if (query.equals(searchField.getText())) newBrochureButton.setDisable(false);
            if (brochures.isEmpty()) {
                searchMessage.setText("Δεν βρέθηκε μπροσούρα με αυτόν τον τίτλο. Επιλέξτε «Νέα μπροσούρα».");
            }
        }, error -> {
            searchButton.setDisable(false);
            busyIndicator.setVisible(false);
            resultsTable.getItems().clear();
            searchMessage.setText(UserMessages.describe(error));
        });
    }

    @FXML
    private void handleSelect() {
        PublicationDTO selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected != null) showPreview(selected, "Υπάρχει ήδη στον κατάλογο του δικτύου.");
    }

    @FXML
    private void handleNewBrochure() {
        formController.setBrochure(searchField.getText());
        showForm();
    }
}
