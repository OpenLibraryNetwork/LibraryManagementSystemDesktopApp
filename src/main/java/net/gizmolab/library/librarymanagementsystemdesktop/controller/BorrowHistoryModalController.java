package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AlertManager;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PaginationHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.TableCellFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Predicate;

@Component
@Scope("prototype")
public class BorrowHistoryModalController {

    private static final Logger logger = LoggerFactory.getLogger(BorrowHistoryModalController.class);

    @Autowired private IBorrowService borrowService;
    @Autowired private I18nManager i18nManager;
    @Autowired private AlertManager alertManager;

    // FXML components
    @FXML private TableView<BorrowDTO> historyTable;
    @FXML private TableColumn<BorrowDTO, String> publicationTitleColumn;
    @FXML private TableColumn<BorrowDTO, String> authorColumn;
    @FXML private TableColumn<BorrowDTO, Integer> copyNumberColumn;
    @FXML private TableColumn<BorrowDTO, String> borrowDateColumn;
    @FXML private TableColumn<BorrowDTO, String> returnDateColumn;
    @FXML private TableColumn<BorrowDTO, String> statusColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Button returnBookButton;
    @FXML private HBox pageButtonsContainer;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Button closeButton;
    @FXML private Label tableStatusLabel;

    // Data management
    private ObservableList<BorrowDTO> masterData;
    private FilteredList<BorrowDTO> filteredData;
    private SortedList<BorrowDTO> sortedData;

    // Pagination state
    private int currentPage = 1;
    private static final int PAGE_SIZE = 15;
    private int totalPages = 1;

    // Context
    private User user;
    private Stage dialogStage;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FMT);
    }

    // Public API

    public void setUser(User user) {
        if (user == null) {
            this.user = null;
            historyTable.setPlaceholder(new Label(i18nManager.getMessage("borrowHistory.noUser")));
            return;
        }
        this.user = user;
        loadData();
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    // FXML lifecycle

    @FXML
    private void initialize() {
        masterData = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(historyTable.comparatorProperty());

        setupTableColumns();
        setupStatusFilter();
        setupSearchFunctionality();
        setupSelectionListener();
        setupPagination();
    }

    private void setupTableColumns() {
        // Publication Title Column
        publicationTitleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPublicationTitle()));
        publicationTitleColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Author Column
        authorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAuthorName()));
        authorColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Copy Number Column
        copyNumberColumn.setCellValueFactory(new PropertyValueFactory<>("copyNumber"));
        copyNumberColumn.setCellFactory(TableCellFactory.createNumericCellFactory());

        // Borrow Date Column
        borrowDateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatDate(cellData.getValue().getBorrowDate())));
        borrowDateColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Return Date Column
        returnDateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(formatDate(cellData.getValue().getReturnDate())));
        returnDateColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Status Column with CSS styling
        statusColumn.setCellValueFactory(cellData -> {
            BorrowDTO borrow = cellData.getValue();
            String status = borrow.getStatus();
            if (status != null) {
                switch (status.toLowerCase()) {
                    case "active": return new SimpleStringProperty(i18nManager.getMessage("borrowHistory.status.active"));
                    case "returned": return new SimpleStringProperty(i18nManager.getMessage("borrowHistory.status.returned"));
                    case "overdue": return new SimpleStringProperty(i18nManager.getMessage("borrowHistory.status.overdue"));
                    default: break; // an unknown status is shown as it is
                }
            }
            return new SimpleStringProperty(status != null ? status : "");
        });
        statusColumn.setCellFactory(column -> new TableCell<BorrowDTO, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                getStyleClass().removeAll("status-active", "status-returned", "status-overdue");

                if (empty || status == null || status.isEmpty()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(status);
                    setTooltip(new Tooltip(status));

                    String activeLabel = i18nManager.getMessage("borrowHistory.status.active");
                    String returnedLabel = i18nManager.getMessage("borrowHistory.status.returned");
                    String overdueLabel = i18nManager.getMessage("borrowHistory.status.overdue");

                    if (status.equals(activeLabel)) {
                        getStyleClass().add("status-active");
                    } else if (status.equals(returnedLabel)) {
                        getStyleClass().add("status-returned");
                    } else if (status.equals(overdueLabel)) {
                        getStyleClass().add("status-overdue");
                    }
                }
            }
        });
    }

    private void setupStatusFilter() {
        if (statusFilterComboBox != null) {
            statusFilterComboBox.getItems().addAll(
                i18nManager.getMessage("borrowHistory.filter.all"),
                i18nManager.getMessage("borrowHistory.filter.active"),
                i18nManager.getMessage("borrowHistory.filter.returned"),
                i18nManager.getMessage("borrowHistory.filter.overdue")
            );
            statusFilterComboBox.setValue(i18nManager.getMessage("borrowHistory.filter.all"));

            statusFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
    }

    private void setupSearchFunctionality() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
    }

    private void setupSelectionListener() {
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> updateReturnButtonState());
    }

    private void updateReturnButtonState() {
        if (returnBookButton != null) {
            BorrowDTO selected = historyTable.getSelectionModel().getSelectedItem();
            boolean canReturn = selected != null && selected.isActive();
            returnBookButton.setDisable(!canReturn);
        }
    }

    private void applyFilters() {
        filteredData.setPredicate(createCombinedPredicate());
        currentPage = 1;
        totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PAGE_SIZE));
        refreshTableForCurrentPage();
    }

    private Predicate<BorrowDTO> createCombinedPredicate() {
        String searchText = searchField != null ? searchField.getText() : null;
        String selectedFilter = statusFilterComboBox != null ? statusFilterComboBox.getValue() : null;

        return dto -> {
            // Status filter
            if (!applyStatusFilter(dto, selectedFilter)) {
                return false;
            }
            // Text search
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = searchText.toLowerCase();
            if (dto.getPublicationTitle() != null && dto.getPublicationTitle().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            if (dto.getAuthorName() != null && dto.getAuthorName().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            return dto.getIsbn() != null && dto.getIsbn().toLowerCase().contains(lowerCaseFilter);
        };
    }

    private boolean applyStatusFilter(BorrowDTO dto, String selectedFilter) {
        if (selectedFilter == null || selectedFilter.equals(i18nManager.getMessage("borrowHistory.filter.all"))) {
            return true;
        }
        String status = dto.getStatus();
        if (status == null) {
            return false;
        }
        if (selectedFilter.equals(i18nManager.getMessage("borrowHistory.filter.active"))) {
            return status.equalsIgnoreCase("Active");
        } else if (selectedFilter.equals(i18nManager.getMessage("borrowHistory.filter.returned"))) {
            return status.equalsIgnoreCase("Returned");
        } else if (selectedFilter.equals(i18nManager.getMessage("borrowHistory.filter.overdue"))) {
            return status.equalsIgnoreCase("Overdue");
        }
        return true;
    }

    private void setupPagination() {
        refreshTableForCurrentPage();
    }

    private void loadData() {
        historyTable.setPlaceholder(new Label(i18nManager.getMessage("borrowHistory.loading")));

        Task<List<BorrowDTO>> loadTask = new Task<>() {
            @Override
            protected List<BorrowDTO> call() {
                return borrowService.getBorrowHistoryByUserAsDTO(user.getUserId());
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<BorrowDTO> dtos = loadTask.getValue();
            masterData.setAll(dtos);

            if (dtos.isEmpty()) {
                historyTable.setPlaceholder(new Label(i18nManager.getMessage("borrowHistory.noRecords")));
            }

            totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PAGE_SIZE));
            currentPage = 1;
            refreshTableForCurrentPage();
        });

        loadTask.setOnFailed(event -> {
            Throwable ex = loadTask.getException();
            logger.error("Failed to load borrow history for user: {}", user.getUserId(), ex);
            String errorMessage = ex != null ? ex.getMessage() : "Unknown error";
            alertManager.showError(i18nManager.getMessage("error.title"),
                    i18nManager.getMessage("borrowHistory.error.load", errorMessage));
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void goToPage(int page) {
        if (page < 1 || page > totalPages) return;
        currentPage = page;
        refreshTableForCurrentPage();
    }

    private void updatePaginationBar() {
        int totalItems = sortedData != null ? sortedData.size() : 0;
        totalPages = (totalItems == 0) ? 1 : (int) Math.ceil((double) totalItems / PAGE_SIZE);

        if (currentPage > totalPages) {
            currentPage = 1;
        }

        if (previousButton != null) previousButton.setDisable(currentPage == 1);
        if (nextButton != null) nextButton.setDisable(currentPage == totalPages);

        if (tableStatusLabel != null) {
            if (totalItems == 0) {
                tableStatusLabel.setText(i18nManager.getMessage("table.placeholder.no.items"));
            } else {
                int fromRecord = (currentPage - 1) * PAGE_SIZE + 1;
                int toRecord = Math.min(currentPage * PAGE_SIZE, totalItems);
                tableStatusLabel.setText(i18nManager.getMessage("status.items.filtered", toRecord - fromRecord + 1, totalItems));
            }
        }

        buildPageButtons();
    }

    private void buildPageButtons() {
        if (pageButtonsContainer == null) return;

        pageButtonsContainer.getChildren().clear();

        List<Integer> visiblePages = PaginationHelper.buildVisiblePages(currentPage, totalPages);

        int prev = 0;
        for (int page : visiblePages) {
            if (prev > 0 && page > prev + 1) {
                Label ellipsis = new Label("...");
                ellipsis.getStyleClass().add("pagination-ellipsis");
                pageButtonsContainer.getChildren().add(ellipsis);
            }

            Button btn = new Button(String.valueOf(page));
            btn.getStyleClass().add("page-button");
            if (page == currentPage) {
                btn.getStyleClass().add("page-button-active");
            }
            final int targetPage = page;
            btn.setOnAction(e -> goToPage(targetPage));
            pageButtonsContainer.getChildren().add(btn);

            prev = page;
        }
    }

    private List<BorrowDTO> getCurrentPageData() {
        if (sortedData == null || sortedData.isEmpty()) return List.of();
        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, sortedData.size());
        if (fromIndex >= sortedData.size()) return List.of();
        return sortedData.subList(fromIndex, toIndex);
    }

    private void refreshTableForCurrentPage() {
        historyTable.setItems(FXCollections.observableArrayList(getCurrentPageData()));
        updatePaginationBar();
        updateReturnButtonState();
    }

    // FXML event handlers

    @FXML
    private void handleReturnBook() {
        BorrowDTO selectedBorrow = historyTable.getSelectionModel().getSelectedItem();
        if (selectedBorrow == null || !selectedBorrow.isActive()) {
            return;
        }

        boolean confirmed = alertManager.showConfirmation(
            i18nManager.getMessage("borrowHistory.confirmReturn"),
            i18nManager.getMessage("borrowHistory.confirmReturn.message")
        );

        if (confirmed) {
            try {
                borrowService.returnItem(selectedBorrow.getId());
                alertManager.showSuccess("success.book.returned");
                loadData();
            } catch (Exception e) {
                logger.error("Failed to return book", e);
                alertManager.showError(i18nManager.getMessage("error.title"),
                        i18nManager.getMessage("error.operation.failed", "Return Book", e.getMessage()));
            }
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handlePrevious() {
        goToPage(currentPage - 1);
    }

    @FXML
    private void handleNext() {
        goToPage(currentPage + 1);
    }
}
