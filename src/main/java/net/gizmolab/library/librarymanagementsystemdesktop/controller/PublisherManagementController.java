package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
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
public class PublisherManagementController extends BaseManagementController<PublisherDTO> {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<PublisherDTO, String> nameColumn;
    @FXML private TableColumn<PublisherDTO, String> bookCountColumn;

    @Override
    protected void setupTableColumns() {
        nameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getName()));
        bookCountColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getBookCount())));
    }

    @Override
    protected Predicate<PublisherDTO> createSearchPredicate(String searchText) {
        if (searchText == null || searchText.isEmpty()) return pub -> true;
        String lower = searchText.toLowerCase();
        return pub -> pub.getName() != null && pub.getName().toLowerCase().contains(lower);
    }

    @Override
    protected List<PublisherDTO> loadDataFromService() {
        try {
            JsonNode response = strapiApiClient.getAllPublishers();
            return DTOConverter.publishersFromJson(response);
        } catch (Exception e) {
            logError("Failed to load publishers", e);
            return List.of();
        }
    }

    @Override
    protected PublisherDTO showAddEditDialog(PublisherDTO item) {
        try {
            var result = fxmlLoaderFactory.loadWithController("/fxml/publisher-form-dialog.fxml");
            PublisherFormController controller = (PublisherFormController) result.getController();
            controller.setPublisher(item);

            Dialog<PublisherDTO> dialog = new Dialog<>();
            dialog.setTitle(item == null ?
                i18nManager.getMessage("publisher.add") :
                i18nManager.getMessage("publisher.edit"));
            dialog.getDialogPane().setContent((javafx.scene.Node) result.getRoot());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

            final PublisherDTO[] savedItem = {null};
            controller.setOnSaveCallback(saved -> {
                savedItem[0] = saved;
                dialog.close();
            });

            dialog.showAndWait();
            return savedItem[0];
        } catch (Exception e) {
            handleException("Failed to open publisher form", e);
            return null;
        }
    }

    @Override
    protected PublisherDTO saveItem(PublisherDTO item) {
        return item; // Handled in form controller
    }

    @Override
    protected void deleteItem(PublisherDTO item) {
        alertManager.showWarning(i18nManager.getMessage("warning.title"),
            i18nManager.getMessage("publisher.delete.notallowed",
                "Publishers can only be deleted by the administrator."));
    }

    @Override protected String getEntityTypeName() { return "Publisher"; }
    @Override protected String getEntityDisplayName(PublisherDTO item) { return item.getName(); }
}
