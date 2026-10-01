package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.MagazineDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.CatalogService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.Issn;
import net.gizmolab.library.librarymanagementsystemdesktop.util.LatestRequestGate;
import net.gizmolab.library.librarymanagementsystemdesktop.util.SearchText;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * "Προσθήκη τεύχους": magazine (ISSN/barcode → catalog or National Library; title → network search;
 * or a new magazine) → its issues in the network → preview of an existing issue or the issue form → copies.
 */
@Controller
@Scope("prototype")
public class AddIssueWizardController extends AddPublicationWizardController {

    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private VBox magazinePane;
    @FXML private TextField codeField;
    @FXML private Button searchButton;
    @FXML private Label codeMessage;
    @FXML private TableView<MagazineDTO> magazinesTable;
    @FXML private TableColumn<MagazineDTO, String> magTitleColumn;
    @FXML private TableColumn<MagazineDTO, String> magIssnColumn;
    @FXML private TableColumn<MagazineDTO, String> magPublisherColumn;
    @FXML private TableColumn<MagazineDTO, String> magInLibraryColumn;
    @FXML private Label magazinesPlaceholder;
    @FXML private Button selectMagazineButton;
    @FXML private Button newMagazineButton;

    @FXML private VBox issuesPane;
    @FXML private Label magazineHeader;
    @FXML private Label magazineDetails;
    @FXML private Label magazineSource;
    @FXML private TableView<PublicationDTO> issuesTable;
    @FXML private TableColumn<PublicationDTO, String> issueNumberColumn;
    @FXML private TableColumn<PublicationDTO, String> issuePeriodColumn;
    @FXML private TableColumn<PublicationDTO, String> issueThemeColumn;
    @FXML private TableColumn<PublicationDTO, String> issueInLibraryColumn;
    @FXML private Label issuesPlaceholder;
    @FXML private Button selectIssueButton;

    private MagazineDTO magazine;
    // A search answer is applied only if the librarian has not moved on (new magazine, another choice).
    private final LatestRequestGate searchGate = new LatestRequestGate();

    /** preselected: the magazine selected on the "Περιοδικά" screen, or null. */
    public static void open(FXMLLoaderFactory factory, Window owner, MagazineDTO preselected) throws Exception {
        openWizard(factory, owner, "/fxml/add-issue-wizard.fxml", "Προσθήκη τεύχους", (AddIssueWizardController wizard) -> {
            if (preselected != null) wizard.useMagazine(preselected, "Περιοδικό της βιβλιοθήκης σας.");
        });
    }

    @Override
    protected String duplicatesMessage() {
        return "Το τεύχος υπάρχει ήδη στον κατάλογο. Χρησιμοποιήστε την υπάρχουσα εγγραφή:";
    }

    @Override
    protected boolean canGoBackOnStart() {
        return issuesPane.isVisible();
    }

    @Override
    protected void backOnStart() {
        shown(issuesPane, false);
        shown(magazinePane, true);
        show(Page.START);
    }

    @Override
    protected void initialize() {
        super.initialize();
        magTitleColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayName()));
        magIssnColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getIssn())));
        magPublisherColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPublisher() != null ? d.getValue().getPublisher().getDisplayName() : ""));
        magInLibraryColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getIssuesInLibrary() > 0 ? d.getValue().getIssuesInLibrary() + " τεύχη" : "—"));
        magazinesTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, m) -> selectMagazineButton.setDisable(m == null));
        magazinesTable.setRowFactory(table -> doubleClickRow(this::handleSelectMagazine));
        magazinesPlaceholder.setText("Πληκτρολογήστε ή σαρώστε το ISSN, ή αναζητήστε με τίτλο.");
        // "Νέο περιοδικό" only after a title search for exactly this text (a not-found ISSN opens it by itself)
        codeField.textProperty().addListener((obs, old, text) -> newMagazineButton.setDisable(true));

        issueNumberColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getIssueNumber())));
        issuePeriodColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getPublicationMonthYear())));
        issueThemeColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getSubtitle())));
        issueInLibraryColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTotalCopies() > 0 ? d.getValue().getTotalCopies() + " αντίτυπα" : "—"));
        issuesTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, i) -> selectIssueButton.setDisable(i == null));
        issuesTable.setRowFactory(table -> doubleClickRow(this::handleSelectIssue));
    }

    private static <T> TableRow<T> doubleClickRow(Runnable action) {
        TableRow<T> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) action.run();
        });
        return row;
    }

    @FXML
    private void handleSearch() {
        String text = codeField.getText() == null ? "" : codeField.getText().trim();
        codeMessage.setText("");
        if (Issn.looksLikeCode(text)) {
            Optional<String> issn = Issn.parseCode(text);
            if (issn.isEmpty()) {
                codeMessage.setText("Μη έγκυρο ISSN ή barcode περιοδικού (το barcode περιοδικού ξεκινά με 977).");
                return;
            }
            runSearch(() -> catalog.lookupIssn(issn.get()), this::onLookup);
        } else if (SearchText.isSearchable(text)) {
            runSearch(() -> catalog.searchMagazines(text), magazines -> onTitleResults(text, magazines));
        } else {
            codeMessage.setText("Πληκτρολογήστε ISSN, barcode ή τουλάχιστον 2 χαρακτήρες του τίτλου.");
        }
    }

    private <T> void runSearch(Callable<T> call, Consumer<T> onResult) {
        long token = searchGate.next();
        searchButton.setDisable(true);
        busyIndicator.setVisible(true);
        BackgroundTasks.run(call, result -> {
            searchButton.setDisable(false);
            busyIndicator.setVisible(false);
            if (searchGate.isLatest(token)) onResult.accept(result);
        }, error -> {
            searchButton.setDisable(false);
            busyIndicator.setVisible(false);
            if (searchGate.isLatest(token)) codeMessage.setText(UserMessages.describe(error));
        });
    }

    private void onLookup(CatalogService.MagazineLookupResult result) {
        switch (result.source()) {
            case CATALOG -> useMagazine(result.magazine(), "Βρέθηκε στον κατάλογο του δικτύου.");
            case NLG -> useMagazine(result.magazine(), "Βρέθηκε στην Εθνική Βιβλιοθήκη και προστέθηκε στον κατάλογο του δικτύου.");
            case NOT_FOUND -> openNewMagazine(result.issn(), null, "Το ISSN " + result.issn()
                    + " δεν βρέθηκε ούτε στον κατάλογο ούτε στην Εθνική Βιβλιοθήκη. Καταχωρίστε το περιοδικό· θα το ελέγξει ο καταλογογράφος.");
            case UNAVAILABLE -> openNewMagazine(result.issn(), null,
                    "Η Εθνική Βιβλιοθήκη δεν απάντησε· καταχωρίστε το περιοδικό τοπικά, θα το ελέγξει ο καταλογογράφος.");
        }
    }

    private void onTitleResults(String searched, List<MagazineDTO> magazines) {
        magazinesTable.getItems().setAll(magazines);
        if (searched.equals(codeField.getText() == null ? "" : codeField.getText().trim())) newMagazineButton.setDisable(false);
        if (magazines.isEmpty()) {
            magazinesPlaceholder.setText("Δεν βρέθηκε περιοδικό με αυτόν τον τίτλο στον κατάλογο του δικτύου. "
                    + "Αν το περιοδικό έχει ISSN, αναζητήστε με αυτό για να βρεθεί στην Εθνική Βιβλιοθήκη· αλλιώς επιλέξτε «Νέο περιοδικό».");
        }
    }

    @FXML
    private void handleSelectMagazine() {
        MagazineDTO selected = magazinesTable.getSelectionModel().getSelectedItem();
        if (selected != null) useMagazine(selected, "Επιλέχθηκε από τον κατάλογο του δικτύου.");
    }

    @FXML
    private void handleNewMagazine() {
        String text = codeField.getText() == null ? "" : codeField.getText().trim();
        openNewMagazine(null, Issn.looksLikeCode(text) ? null : text, null);
    }

    private void openNewMagazine(String lockedIssn, String title, String message) {
        searchGate.next(); // a search still running must not open a second dialog or switch the magazine
        try {
            NewMagazineDialogController.open(fxmlLoaderFactory, codeField.getScene().getWindow(), lockedIssn, title, message)
                    .ifPresent(m -> useMagazine(m, "Περιοδικό του καταλόγου του δικτύου."));
        } catch (Exception e) {
            handleException("Νέο περιοδικό", e);
        }
    }

    /** Step 2: the magazine's issues in the whole network, with this library's copies. */
    private void useMagazine(MagazineDTO chosen, String sourceNote) {
        searchGate.next();
        magazine = chosen;
        magazineHeader.setText(chosen.getDisplayName());
        magazineDetails.setText(details(chosen));
        magazineSource.setText(sourceNote);
        issuesTable.getItems().clear();
        issuesPlaceholder.setText("Φόρτωση τευχών…");
        shown(magazinePane, false);
        shown(issuesPane, true);
        show(Page.START);
        BackgroundTasks.run(() -> catalog.getIssues(chosen.getDocumentId(), null), issues -> {
            if (magazine != chosen) return; // the librarian went back and chose another magazine
            issuesTable.getItems().setAll(issues);
            issuesPlaceholder.setText("Δεν υπάρχουν ακόμη τεύχη αυτού του περιοδικού στο δίκτυο. Επιλέξτε «Νέο τεύχος».");
        }, error -> {
            if (magazine == chosen) issuesPlaceholder.setText(UserMessages.describe(error));
        });
    }

    private static String details(MagazineDTO m) {
        List<String> parts = new ArrayList<>();
        if (m.getIssn() != null) parts.add("ISSN " + m.getIssn());
        if (m.getPublisher() != null) parts.add(m.getPublisher().getDisplayName());
        if (m.getPlace() != null) parts.add(m.getPlace());
        if (m.getPeriodicity() != null) parts.add(m.getPeriodicity());
        return String.join(" · ", parts);
    }

    @FXML
    private void handleSelectIssue() {
        PublicationDTO issue = issuesTable.getSelectionModel().getSelectedItem();
        if (issue != null) showPreview(issue, "Το τεύχος υπάρχει ήδη στον κατάλογο του δικτύου.");
    }

    @FXML
    private void handleNewIssue() {
        formController.setIssue(magazine);
        showForm();
    }

    private static void shown(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
