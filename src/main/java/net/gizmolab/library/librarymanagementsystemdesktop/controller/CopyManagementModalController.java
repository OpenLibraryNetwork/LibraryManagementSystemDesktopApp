package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.CopyDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PublicationDetailFormatter;
import tools.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
@Scope("prototype")
public class CopyManagementModalController extends BaseController {

    @Autowired private StrapiApiClient strapiApiClient;

    @FXML private Label titleLabel;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;
    @FXML private TableView<CopyDTO> copiesTable;
    @FXML private TableColumn<CopyDTO, String> copyNumberColumn;
    @FXML private TableColumn<CopyDTO, String> conditionColumn;
    @FXML private TableColumn<CopyDTO, String> availableColumn;
    @FXML private Label statusLabel;

    private PublicationDTO publication;
    private ObservableList<CopyDTO> copiesData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        copyNumberColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCopyNumber())));
        conditionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCondition()));
        availableColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAvailable() ? "✓" : "✗"));

        copiesTable.setItems(copiesData);

        copiesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            editButton.setDisable(newSel == null);
            deleteButton.setDisable(!PublicationDetailFormatter.canDeleteCopy(newSel));
        });
    }

    public void setPublication(PublicationDTO publication) {
        this.publication = publication;
        if (titleLabel != null) {
            titleLabel.setText("Αντίτυπα: " + publication.getTitle());
        }
        loadCopies();
    }

    private void loadCopies() {
        if (publication == null) return;
        
        Task<List<CopyDTO>> task = new Task<>() {
            @Override
            protected List<CopyDTO> call() throws Exception {
                String libId = AuthService.getCurrentLibraryDocumentId();
                JsonNode response = strapiApiClient.getCopiesForPublication(publication.getDocumentId(), libId);
                return DTOConverter.copiesFromJson(response);
            }
        };

        task.setOnSucceeded(e -> {
            copiesData.setAll(task.getValue());
            updateStatusLabel();
        });

        task.setOnFailed(e -> handleException("Failed to load copies", (Exception) task.getException()));

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void updateStatusLabel() {
        long available = copiesData.stream().filter(CopyDTO::isAvailable).count();
        statusLabel.setText(String.format("%d αντίτυπα (%d διαθέσιμα)", copiesData.size(), available));
    }

    @FXML
    private void handleAdd() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Προσθήκη Αντιτύπου");
        dialog.setHeaderText("Εισάγετε στοιχεία νέου αντιτύπου");
        
        ButtonType saveButtonType = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Spinner<Integer> numberSpinner = new Spinner<>(1, 1000, 1);
        numberSpinner.setEditable(true);
        ComboBox<String> conditionCombo = new ComboBox<>(FXCollections.observableArrayList("NEW", "GOOD", "FAIR", "POOR"));
        conditionCombo.setValue("NEW");

        grid.add(new Label("Copy #:"), 0, 0);
        grid.add(numberSpinner, 1, 0);
        grid.add(new Label("Κατάσταση:"), 0, 1);
        grid.add(conditionCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            int number = numberSpinner.getValue();
            String condition = conditionCombo.getValue();

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    strapiApiClient.createCopy(publication.getDocumentId(), number, condition);
                    return null;
                }
            };

            task.setOnSucceeded(e -> loadCopies());
            task.setOnFailed(e -> handleException("Failed to create copy", (Exception) task.getException()));

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        }
    }

    @FXML
    private void handleEdit() {
        CopyDTO selected = copiesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Αλλαγή Κατάστασης");
        dialog.setHeaderText("Αλλαγή κατάστασης για Copy #" + selected.getCopyNumber());
        
        ButtonType saveButtonType = new ButtonType("Αποθήκευση", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        ComboBox<String> conditionCombo = new ComboBox<>(FXCollections.observableArrayList("NEW", "GOOD", "FAIR", "POOR"));
        conditionCombo.setValue(selected.getCondition());

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(new Label("Κατάσταση:"), conditionCombo);
        dialog.getDialogPane().setContent(vbox);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String newCondition = conditionCombo.getValue();
            if (newCondition.equals(selected.getCondition())) return;

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    strapiApiClient.updateCopyCondition(selected.getDocumentId(), newCondition);
                    return null;
                }
            };

            task.setOnSucceeded(e -> loadCopies());
            task.setOnFailed(e -> handleException("Failed to update copy condition", (Exception) task.getException()));

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        }
    }

    @FXML
    private void handleDelete() {
        CopyDTO selected = copiesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!PublicationDetailFormatter.canDeleteCopy(selected)) {
            alertManager.showWarning(i18nManager.getMessage("warning.title"),
                    "Το αντίτυπο είναι δανεισμένο και δεν μπορεί να διαγραφεί πριν επιστραφεί.");
            return;
        }

        if (showDeleteConfirmation("Αντίτυπο", "Copy #" + selected.getCopyNumber())) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    strapiApiClient.deleteCopy(selected.getDocumentId());
                    return null;
                }
            };

            task.setOnSucceeded(e -> loadCopies());
            task.setOnFailed(e -> handleException("Failed to delete copy", (Exception) task.getException()));

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        }
    }

    @FXML
    private void handleRefresh() {
        loadCopies();
    }

}
