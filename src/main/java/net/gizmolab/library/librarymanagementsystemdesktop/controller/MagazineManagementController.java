package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.MagazineDTO;
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

/**
 * Manages Magazine titles (not issues).
 * Issues are displayed as Publications with type=Περιοδικό in PublicationManagementController.
 */
@Controller
public class MagazineManagementController extends BaseManagementController<MagazineDTO> {

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    @FXML private TableColumn<MagazineDTO, String> titleColumn;
    @FXML private TableColumn<MagazineDTO, String> issnColumn;
    @FXML private TableColumn<MagazineDTO, String> publisherColumn;
    @FXML private TableColumn<MagazineDTO, String> issuesCountColumn;

    @Override
    protected void setupTableColumns() {
        titleColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getTitle()));
        issnColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIssn() != null ?
                data.getValue().getIssn() : ""));
        publisherColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPublisher() != null ?
                data.getValue().getPublisher().getName() : ""));
        issuesCountColumn.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getIssuesCount())));
    }

    @Override
    protected Predicate<MagazineDTO> createSearchPredicate(String searchText) {
        if (searchText == null || searchText.isEmpty()) return mag -> true;
        String lower = searchText.toLowerCase();
        return mag -> {
            if (mag.getTitle() != null && mag.getTitle().toLowerCase().contains(lower)) return true;
            if (mag.getIssn() != null && mag.getIssn().toLowerCase().contains(lower)) return true;
            return false;
        };
    }

    @Override
    protected List<MagazineDTO> loadDataFromService() {
        try {
            JsonNode response = strapiApiClient.getAllMagazines();
            return DTOConverter.magazinesFromJson(response);
        } catch (Exception e) {
            logError("Failed to load magazines", e);
            return List.of();
        }
    }

    @Override
    protected MagazineDTO showAddEditDialog(MagazineDTO item) {
        try {
            var result = fxmlLoaderFactory.loadWithController("/fxml/magazine-form-dialog.fxml");
            MagazineFormController controller = (MagazineFormController) result.getController();
            controller.setMagazine(item);

            Dialog<MagazineDTO> dialog = new Dialog<>();
            dialog.setTitle(item == null ?
                i18nManager.getMessage("magazine.add") :
                i18nManager.getMessage("magazine.edit"));
            dialog.getDialogPane().setContent((javafx.scene.Node) result.getRoot());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

            final MagazineDTO[] savedItem = {null};
            controller.setOnSaveCallback(saved -> {
                savedItem[0] = saved;
                dialog.close();
            });

            dialog.showAndWait();
            return savedItem[0];
        } catch (Exception e) {
            handleException("Failed to open magazine form", e);
            return null;
        }
    }

    @Override
    protected MagazineDTO saveItem(MagazineDTO item) {
        return item; // Handled in form controller
    }

    @Override
    protected void deleteItem(MagazineDTO item) {
        alertManager.showWarning(i18nManager.getMessage("warning.title"),
            i18nManager.getMessage("magazine.delete.notallowed",
                "Magazines can only be deleted by the administrator."));
    }

    @Override protected String getEntityTypeName() { return "Magazine"; }
    @Override protected String getEntityDisplayName(MagazineDTO item) { return item.getTitle(); }
}
