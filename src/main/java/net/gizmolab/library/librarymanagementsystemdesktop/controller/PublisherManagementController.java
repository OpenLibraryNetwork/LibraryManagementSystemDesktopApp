package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.StrapiPageResponse;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import tools.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.function.Predicate;

/**
 * Publishers of the library: publishers of publications that have a copy here.
 * Read-only; "Έντυπα" (and double-click) lists their publications in this library.
 */
@Controller
public class PublisherManagementController extends BaseManagementController<PublisherDTO> {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<PublisherDTO, String> nameColumn;
    @FXML private TableColumn<PublisherDTO, String> addressColumn;
    @FXML private TableColumn<PublisherDTO, String> phoneColumn;
    @FXML private TableColumn<PublisherDTO, String> emailColumn;
    @FXML private TableColumn<PublisherDTO, String> websiteColumn;
    @FXML private TableColumn<PublisherDTO, String> bookCountColumn;

    @Override
    protected boolean isServerSidePagination() {
        return true;
    }

    @Override
    protected void setupTableColumns() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));
        addressColumn.setCellValueFactory(data -> new SimpleStringProperty(nz(data.getValue().getAddress())));
        phoneColumn.setCellValueFactory(data -> new SimpleStringProperty(nz(data.getValue().getPhone())));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(nz(data.getValue().getEmail())));
        websiteColumn.setCellValueFactory(data -> new SimpleStringProperty(nz(data.getValue().getWebsite())));
        bookCountColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getBookCount())));
    }

    @Override
    protected Predicate<PublisherDTO> createSearchPredicate(String searchText) {
        return publisher -> true; // server-side search
    }

    @Override
    protected StrapiPageResponse<PublisherDTO> loadPageFromService(int page, int pageSize, String searchQuery) {
        try {
            JsonNode response = strapiApiClient.getPublishersInLibrary(page, pageSize, searchTerm(searchQuery));
            return DTOConverter.publishersPageFromJson(response);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logError("Failed to load publishers page", e);
            return StrapiPageResponse.failed(e);
        }
    }

    /** The server needs at least 2 characters; shorter input means "no filter". */
    private static String searchTerm(String query) {
        return (query != null && query.trim().length() >= 2) ? query.trim() : null;
    }

    /** "Έντυπα" button and double-click. */
    @Override
    protected void editSelectedItem() {
        PublisherDTO selected = getSelectedItem();
        if (selected == null) return;
        try {
            var result = fxmlLoaderFactory.<Parent, PublisherPublicationsModalController>loadWithController(
                    "/fxml/publisher-publications-modal.fxml");
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tableView.getScene().getWindow());
            stage.setTitle(i18nManager.getMessage("publisherPublications.title", selected.getDisplayName()));
            Scene scene = new Scene(result.getRoot(), 800, 600);
            StylesheetHelper.applyModernTheme(scene);
            stage.setScene(scene);
            PublisherPublicationsModalController controller = result.getController();
            controller.setDialogStage(stage);
            controller.setPublisher(selected);
            stage.showAndWait();
        } catch (Exception e) {
            handleException("Άνοιγμα εντύπων εκδότη", e);
        }
    }

    // Read-only: no add, edit or delete of publishers from the library client.
    @Override protected void addNewItem() { /* read-only here */ }
    @Override protected void deleteSelectedItems() { /* read-only here */ }
    @Override protected PublisherDTO showAddEditDialog(PublisherDTO item) { return null; }
    @Override protected PublisherDTO saveItem(PublisherDTO item) { return item; }
    @Override protected void deleteItem(PublisherDTO item) { /* read-only here */ }

    private static String nz(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim(); // Biblionet addresses contain line breaks
    }

    @Override protected String getEntityTypeName() { return "Publisher"; }
    @Override protected String getEntityDisplayName(PublisherDTO item) { return item.getDisplayName(); }
}
