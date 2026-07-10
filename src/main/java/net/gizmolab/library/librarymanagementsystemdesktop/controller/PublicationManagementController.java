package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.function.Predicate;

@Controller
public class PublicationManagementController extends BaseManagementController<PublicationDTO> {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<PublicationDTO, String> titleColumn;
    @FXML private TableColumn<PublicationDTO, String> typeColumn;
    @FXML private TableColumn<PublicationDTO, String> authorColumn;
    @FXML private TableColumn<PublicationDTO, String> publisherColumn;
    @FXML private TableColumn<PublicationDTO, String> isbnColumn;
    @FXML private TableColumn<PublicationDTO, String> yearColumn;
    @FXML private TableColumn<PublicationDTO, String> copiesColumn;

    @FXML private ComboBox<String> typeFilterComboBox;

    private String currentTypeFilter = null; // null = all types

    @Override
    protected void setupTableColumns() {
        titleColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getTitle()));
        typeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getType()));
        authorColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getAuthorNames() != null ?
                data.getValue().getAuthorNames() : ""));
        publisherColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPublisher() != null ?
                data.getValue().getPublisher().getName() : ""));
        isbnColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIsbn() != null ?
                data.getValue().getIsbn() : ""));
        yearColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getYearPublished() != null ?
                String.valueOf(data.getValue().getYearPublished()) : ""));
        copiesColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getAvailableCopies() +
                "/" + data.getValue().getTotalCopies()));

        // Setup type filter
        if (typeFilterComboBox != null) {
            typeFilterComboBox.setItems(FXCollections.observableArrayList(
                "Όλα", "Βιβλίο", "Μπροσούρα", "Περιοδικό"
            ));
            typeFilterComboBox.setValue("Όλα");
            typeFilterComboBox.setOnAction(e -> {
                String selected = typeFilterComboBox.getValue();
                currentTypeFilter = "Όλα".equals(selected) ? null : selected;
                refreshData();
            });
        }
    }

    @Override
    protected Predicate<PublicationDTO> createSearchPredicate(String searchText) {
        if (searchText == null || searchText.isEmpty()) return pub -> true;
        String lower = searchText.toLowerCase();
        return pub -> {
            if (pub.getTitle() != null && pub.getTitle().toLowerCase().contains(lower)) return true;
            if (pub.getAuthorNames() != null && pub.getAuthorNames().toLowerCase().contains(lower)) return true;
            if (pub.getPublisher() != null && pub.getPublisher().getName() != null &&
                pub.getPublisher().getName().toLowerCase().contains(lower)) return true;
            if (pub.getIsbn() != null && pub.getIsbn().toLowerCase().contains(lower)) return true;
            return false;
        };
    }

    @Override
    protected List<PublicationDTO> loadDataFromService() {
        try {
            JsonNode response;
            if (currentTypeFilter != null) {
                response = strapiApiClient.searchByType(currentTypeFilter);
            } else {
                response = strapiApiClient.get(
                    "/api/books?populate=authors,publisher,copies&pagination[pageSize]=100");
            }
            return DTOConverter.publicationsFromJson(response);
        } catch (Exception e) {
            logError("Failed to load publications", e);
            return List.of();
        }
    }

    @Override
    protected PublicationDTO showAddEditDialog(PublicationDTO item) {
        try {
            var result = fxmlLoaderFactory.loadWithController("/fxml/publication-form-dialog.fxml");
            PublicationFormController controller = (PublicationFormController) result.getController();
            controller.setPublication(item);
            if (currentTypeFilter != null) {
                controller.setDefaultType(currentTypeFilter);
            }

            Dialog<PublicationDTO> dialog = new Dialog<>();
            dialog.setTitle(item == null ?
                i18nManager.getMessage("publication.add") :
                i18nManager.getMessage("publication.edit"));
            dialog.getDialogPane().setContent((javafx.scene.Node) result.getRoot());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

            final PublicationDTO[] savedItem = {null};
            controller.setOnSaveCallback(saved -> {
                savedItem[0] = saved;
                dialog.close();
            });

            dialog.showAndWait();
            return savedItem[0];
        } catch (Exception e) {
            handleException("Failed to open publication form", e);
            return null;
        }
    }

    @Override
    protected PublicationDTO saveItem(PublicationDTO item) {
        return item; // Handled inside PublicationFormController
    }

    @Override
    protected void deleteItem(PublicationDTO item) {
        alertManager.showWarning(i18nManager.getMessage("warning.title"),
            i18nManager.getMessage("publication.delete.notallowed",
                "Publications can only be deleted by the administrator."));
    }

    @Override protected String getEntityTypeName() { return "Publication"; }
    @Override protected String getEntityDisplayName(PublicationDTO item) {
        return item.getTitle();
    }
}
