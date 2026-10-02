package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import net.gizmolab.library.librarymanagementsystemdesktop.util.TableCellFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;

@Component
public class BorrowManagementController extends BaseManagementController<BorrowDTO> {

    private static final Logger logger = LoggerFactory.getLogger(BorrowManagementController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired private IBorrowService borrowService;

    // FXML Components specific to borrow management
    @FXML private Label titleLabel;
    @FXML private TableView<BorrowDTO> borrowsTable;
    @FXML private TableColumn<BorrowDTO, String> userColumn;
    @FXML private TableColumn<BorrowDTO, String> publicationTitleColumn;
    @FXML private TableColumn<BorrowDTO, String> publicationTypeColumn;
    @FXML private TableColumn<BorrowDTO, String> authorColumn;
    @FXML private TableColumn<BorrowDTO, Integer> copyNumberColumn;
    @FXML private TableColumn<BorrowDTO, String> borrowDateColumn;
    @FXML private TableColumn<BorrowDTO, String> dueDateColumn;
    @FXML private TableColumn<BorrowDTO, String> returnDateColumn;
    @FXML private TableColumn<BorrowDTO, String> statusColumn;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Button returnButton;
    @FXML private Label tableStatusLabel;

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FMT);
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        this.tableView = borrowsTable;
        super.initialize(location, resources);
        setupPagination();
        setupStatusFilter();
        updateUI();
    }

    @Override
    protected void setupTableColumns() {
        // User Column
        userColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getUserFullName()));
        userColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Publication Title Column
        publicationTitleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPublicationTitle()));
        publicationTitleColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Publication Type Column
        publicationTypeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getPublicationType()));
        publicationTypeColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Author Column
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authorName"));
        authorColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Copy Number Column
        copyNumberColumn.setCellValueFactory(new PropertyValueFactory<>("copyNumber"));
        copyNumberColumn.setCellFactory(TableCellFactory.createNumericCellFactory());

        // Borrow Date Column
        borrowDateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatDate(cellData.getValue().getBorrowDate())));
        borrowDateColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Due Date Column
        dueDateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatDate(cellData.getValue().getDueDate())));
        dueDateColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Return Date Column
        returnDateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatDate(cellData.getValue().getReturnDate())));
        returnDateColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Status Column with styling
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new TableCell<BorrowDTO, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(status);
                    setStyle("-fx-text-overrun: ellipsis;");
                    setTooltip(new Tooltip(status));

                    getStyleClass().removeAll("status-active", "status-returned", "status-overdue");
                    switch (status.toLowerCase()) {
                        case "active":
                            getStyleClass().add("status-active");
                            break;
                        case "returned":
                            getStyleClass().add("status-returned");
                            break;
                        case "overdue":
                            getStyleClass().add("status-overdue");
                            break;
                        default:
                            break; // an unknown status gets no colour
                    }
                }
            }
        });

        // Set table placeholder
        borrowsTable.setPlaceholder(new Label(i18nManager.getMessage("status.loading.borrows")));

        // Setup selection listener for return button
        borrowsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> updateButtonStates());
    }

    private void setupStatusFilter() {
        statusFilterComboBox.getItems().addAll(
            i18nManager.getMessage("filter.status.all"),
            i18nManager.getMessage("borrow.active"),
            i18nManager.getMessage("borrow.returned"),
            i18nManager.getMessage("borrow.overdue")
        );
        statusFilterComboBox.setValue(i18nManager.getMessage("filter.status.all"));

        statusFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (filteredData != null) {
                filteredData.setPredicate(createSearchPredicate(searchField.getText()));
            }
        });
    }

    @Override
    protected void updateButtonStates() {
        super.updateButtonStates();

        BorrowDTO selectedBorrow = borrowsTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selectedBorrow != null;
        boolean canReturn = hasSelection && selectedBorrow.isActive();

        if (returnButton != null) {
            returnButton.setDisable(!canReturn);
        }
    }

    @Override
    protected Predicate<BorrowDTO> createSearchPredicate(String searchText) {
        return borrow -> {
            if (searchText == null || searchText.isEmpty()) {
                return applyStatusFilter(borrow);
            }

            String lowerCaseFilter = searchText.toLowerCase();
            boolean matchesSearch = false;

            if (borrow.getUserFullName() != null &&
                borrow.getUserFullName().toLowerCase().contains(lowerCaseFilter)) {
                matchesSearch = true;
            }
            if (borrow.getPublicationTitle() != null &&
                borrow.getPublicationTitle().toLowerCase().contains(lowerCaseFilter)) {
                matchesSearch = true;
            }
            if (borrow.getAuthorName() != null &&
                borrow.getAuthorName().toLowerCase().contains(lowerCaseFilter)) {
                matchesSearch = true;
            }
            if (borrow.getIsbn() != null &&
                borrow.getIsbn().toLowerCase().contains(lowerCaseFilter)) {
                matchesSearch = true;
            }

            return matchesSearch && applyStatusFilter(borrow);
        };
    }

    private boolean applyStatusFilter(BorrowDTO borrow) {
        String selectedStatus = statusFilterComboBox.getValue();
        if (selectedStatus == null || selectedStatus.equals(i18nManager.getMessage("filter.status.all"))) {
            return true;
        }

        String borrowStatus = borrow.getStatus();
        if (borrowStatus == null) {
            return false;
        }

        if (selectedStatus.equals(i18nManager.getMessage("borrow.active"))) {
            return borrowStatus.equalsIgnoreCase("Active");
        } else if (selectedStatus.equals(i18nManager.getMessage("borrow.returned"))) {
            return borrowStatus.equalsIgnoreCase("Returned");
        } else if (selectedStatus.equals(i18nManager.getMessage("borrow.overdue"))) {
            return borrowStatus.equalsIgnoreCase("Overdue");
        }

        return true;
    }

    @Override
    protected List<BorrowDTO> loadDataFromService() {
        return borrowService.getAllBorrowsAsDTO();
    }

    @Override
    protected BorrowDTO showAddEditDialog(BorrowDTO borrow) {
        if (borrow != null) {
            logger.warn("Editing borrows is not supported");
            return null;
        }
        try {
            FXMLLoaderFactory fxmlLoaderFactory =
                getApplicationContext().getBean(FXMLLoaderFactory.class);

            var loadResult = fxmlLoaderFactory.<javafx.scene.Parent, BorrowFormControllerNew>loadWithController("/fxml/borrow-form-dialog.fxml");

            Stage dialogStage = new Stage();
            dialogStage.setTitle(i18nManager.getMessage("borrow.addBorrow"));
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(borrowsTable.getScene().getWindow());

            Scene dialogScene = new Scene(loadResult.getRoot());
            StylesheetHelper.applyModernTheme(dialogScene);

            dialogStage.setScene(dialogScene);
            dialogStage.setResizable(false);
            dialogStage.sizeToScene();

            BorrowFormControllerNew controller = loadResult.getController();
            controller.setOnSaveCallback(v -> refreshData());

            dialogStage.showAndWait();
            return null;

        } catch (Exception e) {
            logError("Failed to open borrow dialog", e);
            showError(i18nManager.getMessage("error.title"),
                     "Failed to open borrow dialog: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected BorrowDTO saveItem(BorrowDTO borrow) {
        logger.warn("saveItem should not be called directly for borrows");
        return null;
    }

    @Override
    protected void deleteItem(BorrowDTO borrow) {
        throw new UnsupportedOperationException("Borrow deletion not supported through UI");
    }

    @FXML
    private void handleAddBorrow() {
        addNewItem();
    }

    @FXML
    private void handleDeleteBorrow() {
        deleteSelectedItems();
    }

    @FXML
    private void handleReturnBook() {
        BorrowDTO selectedBorrow = borrowsTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow != null && selectedBorrow.isActive()) {
            boolean confirmed = alertManager.showConfirmation(
                i18nManager.getMessage("borrow.confirmReturn"),
                i18nManager.getMessage("borrow.confirmReturn.message")
            );

            if (confirmed) {
                executeWithExceptionHandling("return book", () -> {
                    borrowService.returnItem(selectedBorrow.getId());
                    refreshData();
                    alertManager.showSuccess("success.book.returned");
                });
            }
        }
    }

    public void updateUI() {
        Platform.runLater(() -> {
            if (titleLabel != null) {
                titleLabel.setText(i18nManager.getMessage("borrow.borrowList"));
            }
            if (addButton != null) {
                addButton.setText(i18nManager.getMessage("borrow.addBorrow"));
            }
            if (returnButton != null) {
                returnButton.setText(i18nManager.getMessage("borrow.returnBook"));
            }
            if (deleteButton != null) {
                deleteButton.setText(i18nManager.getMessage("common.delete"));
            }
            if (searchField != null) {
                searchField.setPromptText(i18nManager.getMessage("common.search"));
            }

            if (userColumn != null) userColumn.setText(i18nManager.getMessage("borrow.user"));
            if (publicationTitleColumn != null) publicationTitleColumn.setText(i18nManager.getMessage("borrow.book"));
            if (publicationTypeColumn != null) publicationTypeColumn.setText(i18nManager.getMessage("borrow.type"));
            if (authorColumn != null) authorColumn.setText(i18nManager.getMessage("book.author"));
            if (copyNumberColumn != null) copyNumberColumn.setText(i18nManager.getMessage("borrow.copyNumber"));
            if (borrowDateColumn != null) borrowDateColumn.setText(i18nManager.getMessage("borrow.borrowDate"));
            if (dueDateColumn != null) dueDateColumn.setText(i18nManager.getMessage("borrow.dueDate"));
            if (returnDateColumn != null) returnDateColumn.setText(i18nManager.getMessage("borrow.returnDate"));
            if (statusColumn != null) statusColumn.setText(i18nManager.getMessage("borrow.status"));

            updateStatusLabel();
        });
    }

    @Override
    protected void updateStatusLabel() {
        if (tableStatusLabel != null) {
            int totalCount = masterData.size();
            int filteredCount = filteredData.size();

            String status;
            if (totalCount == filteredCount) {
                status = getLocalizedMessage("status.items.total", totalCount);
            } else {
                status = getLocalizedMessage("status.items.filtered", filteredCount, totalCount);
            }

            tableStatusLabel.setText(status);
        }
    }

    @Override
    protected String getLocalizedMessage(String key, Object... args) {
        return i18nManager.getMessage(key, args);
    }

    @Override
    protected String getEntityTypeName() {
        return i18nManager.getMessage("borrow.borrowBook");
    }

    @Override
    protected String getEntityDisplayName(BorrowDTO borrow) {
        return borrow.getPublicationTitle() + " - " + borrow.getUserFullName();
    }

    @Override
    protected void editSelectedItem() {
        logger.info("Editing borrows is not supported");
    }
}
