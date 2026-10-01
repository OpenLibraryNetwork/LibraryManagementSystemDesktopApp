package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.MagazineDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.StrapiPageResponse;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.CatalogService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.BackgroundTasks;
import net.gizmolab.library.librarymanagementsystemdesktop.util.LatestRequestGate;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.function.Predicate;

/**
 * Magazines of the library (those with an issue that has a copy here) and, below, the selected
 * magazine's issues in the library. Read-only except "Προσθήκη τεύχους" and the copies of an issue.
 */
@Controller
public class MagazineManagementController extends BaseManagementController<MagazineDTO> {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private CatalogService catalog;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<MagazineDTO, String> titleColumn;
    @FXML private TableColumn<MagazineDTO, String> issnColumn;
    @FXML private TableColumn<MagazineDTO, String> publisherColumn;
    @FXML private TableColumn<MagazineDTO, String> issuesColumn;
    @FXML private Label issuesTitleLabel;
    @FXML private Button viewIssueButton;
    @FXML private TableView<PublicationDTO> issuesTable;
    @FXML private TableColumn<PublicationDTO, String> issueNumberColumn;
    @FXML private TableColumn<PublicationDTO, String> issuePeriodColumn;
    @FXML private TableColumn<PublicationDTO, String> issueThemeColumn;
    @FXML private TableColumn<PublicationDTO, String> issueCopiesColumn;
    @FXML private Label issuesPlaceholder;

    private final LatestRequestGate issuesGate = new LatestRequestGate();

    @Override
    protected boolean isServerSidePagination() {
        return true;
    }

    @Override
    protected void setupTableColumns() {
        titleColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayName()));
        issnColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getIssn())));
        publisherColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPublisher() != null ? d.getValue().getPublisher().getDisplayName() : ""));
        issuesColumn.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIssuesInLibrary())));

        issueNumberColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getIssueNumber())));
        issuePeriodColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getPublicationMonthYear())));
        issueThemeColumn.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().getSubtitle())));
        issueCopiesColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAvailableCopies() + "/" + d.getValue().getTotalCopies()));
        issuesTable.setRowFactory(table -> {
            TableRow<PublicationDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) handleViewIssue();
            });
            return row;
        });
        issuesTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, issue) -> viewIssueButton.setDisable(issue == null));
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, magazine) -> loadIssues(magazine));
    }

    private void loadIssues(MagazineDTO magazine) {
        long token = issuesGate.next();
        issuesTable.getItems().clear();
        issuesPlaceholder.setText(i18nManager.getMessage("table.placeholder.no.issues"));
        String heading = i18nManager.getMessage("magazine.issuesOf");
        issuesTitleLabel.setText(magazine == null ? heading : heading + ": " + magazine.getDisplayName());
        if (magazine == null) return;
        BackgroundTasks.run(() -> catalog.getIssues(magazine.getId(), AuthService.getCurrentLibraryId()), issues -> {
            if (issuesGate.isLatest(token)) issuesTable.getItems().setAll(issues);
        }, error -> {
            if (issuesGate.isLatest(token)) issuesPlaceholder.setText(UserMessages.describe(error));
        });
    }

    @FXML
    private void handleViewIssue() {
        PublicationDTO issue = issuesTable.getSelectionModel().getSelectedItem();
        if (issue == null) return;
        try {
            PublicationDetailWindow.open(fxmlLoaderFactory, issuesTable.getScene().getWindow(), issue);
            loadIssues(getSelectedItem()); // copies may have changed
        } catch (Exception e) {
            handleException("Άνοιγμα τεύχους", e);
        }
    }

    @Override
    protected Predicate<MagazineDTO> createSearchPredicate(String searchText) {
        return magazine -> true; // server-side search
    }

    @Override
    protected StrapiPageResponse<MagazineDTO> loadPageFromService(int page, int pageSize, String searchQuery) {
        try {
            String q = (searchQuery != null && searchQuery.trim().length() >= 2) ? searchQuery.trim() : null;
            return DTOConverter.magazinesPageFromJson(strapiApiClient.getMagazinesInLibrary(page, pageSize, q));
        } catch (Exception e) {
            logError("Failed to load magazines page", e);
            return StrapiPageResponse.failed(e);
        }
    }

    /** "Προσθήκη τεύχους" and Ctrl+N: the selected magazine (if any) is the wizard's starting point. */
    @Override
    protected void addNewItem() {
        MagazineDTO selected = getSelectedItem();
        openAddIssueWizard(selected);
        Long keep = selected != null ? selected.getId() : null;
        refreshData();
        Platform.runLater(() -> reselect(keep)); // the refresh replaced the rows and cleared the selection
    }

    /** Selects the magazine with this id again (its issues reload through the selection listener). */
    private void reselect(Long magazineId) {
        if (magazineId == null) return;
        tableView.getItems().stream()
                .filter(m -> magazineId.equals(m.getId()))
                .findFirst()
                .ifPresent(m -> tableView.getSelectionModel().select(m));
    }

    protected void openAddIssueWizard(MagazineDTO preselected) {
        try {
            AddIssueWizardController.open(fxmlLoaderFactory, tableView.getScene().getWindow(), preselected);
        } catch (Exception e) {
            handleException("Προσθήκη τεύχους", e);
        }
    }

    // Magazines are corrected by the cataloguer (Strapi admin), never from the library client.
    @Override protected void editSelectedItem() {}
    @Override protected void deleteSelectedItems() {}
    @Override protected MagazineDTO showAddEditDialog(MagazineDTO item) { return null; }
    @Override protected MagazineDTO saveItem(MagazineDTO item) { return item; }
    @Override protected void deleteItem(MagazineDTO item) {}

    @Override protected String getEntityTypeName() { return "Magazine"; }
    @Override protected String getEntityDisplayName(MagazineDTO item) { return item.getDisplayName(); }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
