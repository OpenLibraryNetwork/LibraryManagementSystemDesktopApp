package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.StrapiPageResponse;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import tools.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Predicate;

/**
 * Library publications of one type (sub-project 2β: "Βιβλία" and "Μπροσούρες" are separate screens).
 * Read-only except copies (publication window) and the type's own add wizard.
 */
public abstract class PublicationListController extends BaseManagementController<PublicationDTO> {

    @Autowired protected StrapiApiClient strapiApiClient;
    @Autowired protected FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<PublicationDTO, String> titleColumn;
    @FXML private TableColumn<PublicationDTO, String> authorColumn;
    @FXML private TableColumn<PublicationDTO, String> publisherColumn;
    @FXML private TableColumn<PublicationDTO, String> isbnColumn; // books only
    @FXML private TableColumn<PublicationDTO, String> yearColumn;
    @FXML private TableColumn<PublicationDTO, String> copiesColumn;

    /** "Βιβλίο" or "Μπροσούρα". */
    protected abstract String publicationType();

    /** Opens this type's add wizard (modal); the list is refreshed afterwards. */
    protected abstract void openAddWizard();

    @Override
    protected boolean isServerSidePagination() {
        return true;
    }

    @Override
    protected void setupTableColumns() {
        titleColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        authorColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAuthorNames() != null ? d.getValue().getAuthorNames() : ""));
        publisherColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPublisher() != null ? d.getValue().getPublisher().getDisplayName() : ""));
        if (isbnColumn != null) {
            isbnColumn.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getIsbn() != null ? d.getValue().getIsbn() : ""));
        }
        yearColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getYearPublished() != null ? String.valueOf(d.getValue().getYearPublished()) : ""));
        copiesColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAvailableCopies() + "/" + d.getValue().getTotalCopies()));
    }

    @Override
    protected Predicate<PublicationDTO> createSearchPredicate(String searchText) {
        return pub -> true; // server-side search
    }

    @Override
    protected StrapiPageResponse<PublicationDTO> loadPageFromService(int page, int pageSize, String searchQuery) {
        try {
            JsonNode response = strapiApiClient.getPublicationsPaginated(page, pageSize, publicationType(), searchQuery);
            return DTOConverter.publicationsPageFromJson(response);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logError("Failed to load " + publicationType() + " page", e);
            return StrapiPageResponse.failed(e);
        }
    }

    /** "Προβολή" and double-click: read-only publication window with the library's copies. */
    @Override
    protected void editSelectedItem() {
        PublicationDTO selected = getSelectedItem();
        if (selected == null) return;
        try {
            PublicationDetailWindow.open(fxmlLoaderFactory, tableView.getScene().getWindow(), selected);
            refreshData();
        } catch (Exception e) {
            handleException("Άνοιγμα εντύπου", e);
        }
    }

    /** "Προσθήκη …" button and Ctrl+N. */
    @Override
    protected void addNewItem() {
        openAddWizard();
        refreshData();
    }

    // Libraries never edit or delete publications.
    @Override protected void deleteSelectedItems() {}
    @Override protected PublicationDTO showAddEditDialog(PublicationDTO item) { return null; }
    @Override protected PublicationDTO saveItem(PublicationDTO item) { return item; }
    @Override protected void deleteItem(PublicationDTO item) {}

    @Override protected String getEntityTypeName() { return publicationType(); }
    @Override protected String getEntityDisplayName(PublicationDTO item) { return item.getTitle(); }
}
