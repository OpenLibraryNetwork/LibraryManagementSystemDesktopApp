package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PersonDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.StrapiPageResponse;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Authors of the library: persons with the author role in a publication that has a copy here.
 * Read-only; "Έργα" (and double-click) lists their works in this library.
 */
@Controller
public class AuthorManagementController extends BaseManagementController<PersonDTO> {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<PersonDTO, String> nameColumn;
    @FXML private TableColumn<PersonDTO, String> lifespanColumn;
    @FXML private TableColumn<PersonDTO, String> bookCountColumn;

    @Override
    protected boolean isServerSidePagination() {
        return true;
    }

    @Override
    protected void setupTableColumns() {
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));
        lifespanColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLifespan()));
        bookCountColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getBookCount())));
    }

    @Override
    protected Predicate<PersonDTO> createSearchPredicate(String searchText) {
        return person -> true; // server-side search
    }

    @Override
    protected StrapiPageResponse<PersonDTO> loadPageFromService(int page, int pageSize, String searchQuery) {
        try {
            JsonNode response = strapiApiClient.getAuthorsInLibrary(page, pageSize, searchTerm(searchQuery));
            return DTOConverter.personsPageFromJson(response);
        } catch (Exception e) {
            logError("Failed to load authors page", e);
            return StrapiPageResponse.failed(e);
        }
    }

    /** The server needs at least 2 characters; shorter input means "no filter". */
    private static String searchTerm(String query) {
        return (query != null && query.trim().length() >= 2) ? query.trim() : null;
    }

    /** "Έργα" button and double-click. */
    @Override
    protected void editSelectedItem() {
        PersonDTO selected = getSelectedItem();
        if (selected == null) return;
        try {
            var result = fxmlLoaderFactory.<Parent, AuthorBooksModalController>loadWithController("/fxml/author-books-modal.fxml");
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tableView.getScene().getWindow());
            stage.setTitle(i18nManager.getMessage("authorBooks.title", selected.getDisplayName()));
            Scene scene = new Scene(result.getRoot(), 800, 600);
            StylesheetHelper.applyModernTheme(scene);
            stage.setScene(scene);
            AuthorBooksModalController controller = result.getController();
            controller.setDialogStage(stage);
            controller.setAuthor(selected);
            stage.showAndWait();
        } catch (Exception e) {
            handleException("Άνοιγμα έργων συγγραφέα", e);
        }
    }

    // Read-only: no add, edit or delete of persons from the library client.
    @Override protected void addNewItem() {}
    @Override protected void deleteSelectedItems() {}
    @Override protected PersonDTO showAddEditDialog(PersonDTO item) { return null; }
    @Override protected PersonDTO saveItem(PersonDTO item) { return item; }
    @Override protected void deleteItem(PersonDTO item) {}

    @Override protected String getEntityTypeName() { return "Author"; }
    @Override protected String getEntityDisplayName(PersonDTO item) { return item.getDisplayName(); }
}
