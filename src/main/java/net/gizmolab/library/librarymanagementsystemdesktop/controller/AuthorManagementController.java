package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.AuthorDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.function.Predicate;

@Controller
public class AuthorManagementController extends BaseManagementController<AuthorDTO> {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<AuthorDTO, String> nameColumn;
    @FXML private TableColumn<AuthorDTO, String> firstnameColumn;
    @FXML private TableColumn<AuthorDTO, String> lastnameColumn;
    @FXML private TableColumn<AuthorDTO, String> bookCountColumn;

    @Override
    protected void setupTableColumns() {
        nameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDisplayName()));
        firstnameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getFirstname() != null ?
                data.getValue().getFirstname() : ""));
        lastnameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getLastname() != null ?
                data.getValue().getLastname() : ""));
        bookCountColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getBookCount())));
    }

    @Override
    protected Predicate<AuthorDTO> createSearchPredicate(String searchText) {
        if (searchText == null || searchText.isEmpty()) return author -> true;
        String lower = searchText.toLowerCase();
        return author -> {
            if (author.getName() != null && author.getName().toLowerCase().contains(lower)) return true;
            if (author.getFirstname() != null && author.getFirstname().toLowerCase().contains(lower)) return true;
            if (author.getLastname() != null && author.getLastname().toLowerCase().contains(lower)) return true;
            return false;
        };
    }

    @Override
    protected List<AuthorDTO> loadDataFromService() {
        try {
            JsonNode response = strapiApiClient.getAllAuthors();
            return DTOConverter.authorsFromJson(response);
        } catch (Exception e) {
            logError("Failed to load authors", e);
            return List.of();
        }
    }

    @Override
    protected AuthorDTO showAddEditDialog(AuthorDTO item) {
        try {
            var result = fxmlLoaderFactory.loadWithController("/fxml/author-form-dialog.fxml");
            AuthorFormController controller = (AuthorFormController) result.getController();
            controller.setAuthor(item);

            Dialog<AuthorDTO> dialog = new Dialog<>();
            dialog.setTitle(item == null ?
                i18nManager.getMessage("author.add") :
                i18nManager.getMessage("author.edit"));
            dialog.getDialogPane().setContent((javafx.scene.Node) result.getRoot());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

            final AuthorDTO[] savedItem = {null};
            controller.setOnSaveCallback(saved -> {
                savedItem[0] = saved;
                dialog.close();
            });

            dialog.showAndWait();
            return savedItem[0];
        } catch (Exception e) {
            handleException("Failed to open author form", e);
            return null;
        }
    }

    @Override
    protected AuthorDTO saveItem(AuthorDTO item) {
        // Handled inside AuthorFormController
        return item;
    }

    @Override
    protected void deleteItem(AuthorDTO item) {
        // Authors cannot be deleted by librarians (admin only)
        alertManager.showWarning(i18nManager.getMessage("warning.title"),
            i18nManager.getMessage("author.delete.notallowed",
                "Authors can only be deleted by the administrator."));
    }

    @Override protected String getEntityTypeName() { return "Author"; }
    @Override protected String getEntityDisplayName(AuthorDTO item) {
        return item.getDisplayName();
    }
}
