package net.gizmolab.library.librarymanagementsystemdesktop.controller.base;

import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.KeyboardAccessibilityHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PaginationHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Base controller for management views that display data in tables with CRUD operations.
 * Provides common functionality for search, filtering, and table management.
 * 
 * @param <T> The type of entity being managed
 */
public abstract class BaseManagementController<T> extends BaseController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(BaseManagementController.class);

    // Common FXML components that should be present in management views
    @FXML protected TextField searchField;
    @FXML protected TableView<T> tableView;
    @FXML protected Button addButton;
    @FXML protected Button editButton;
    @FXML protected Button deleteButton;
    @FXML protected Button refreshButton;
    @FXML protected Label statusLabel;

    // Pagination FXML components
    @FXML protected HBox pageButtonsContainer;
    @FXML protected Button previousButton;
    @FXML protected Button nextButton;

    // Pagination state
    protected int currentPage = 1;
    protected int pageSize = 15;
    protected int totalPages = 1;

    // Data management
    protected ObservableList<T> masterData = FXCollections.observableArrayList();
    protected FilteredList<T> filteredData;
    protected SortedList<T> sortedData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;
        
        setupTableView();
        setupSearchFunctionality();
        setupButtons();
        setupKeyboardShortcuts();
        
        // Load initial data
        refreshData();
        
        logInfo("Management controller initialized: %s", this.getClass().getSimpleName());
    }

    /**
     * Sets up the table view with columns and selection behavior.
     */
    protected void setupTableView() {
        // Setup table columns - to be implemented by subclasses
        setupTableColumns();
        
        // Setup filtered and sorted lists
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        
        tableView.setItems(sortedData);
        
        // Enable multiple selection
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Update button states when selection changes
        tableView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> updateButtonStates()
        );
        
        // Handle double-click to edit
        tableView.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editSelectedItem();
                }
            });
            return row;
        });
    }

    /**
     * Sets up search functionality with real-time filtering.
     */
    protected void setupSearchFunctionality() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(createSearchPredicate(newValue));
                updateStatusLabel();
                updateTablePlaceholder();
            });
        }
    }

    /**
     * Sets up button event handlers and initial states.
     */
    protected void setupButtons() {
        if (addButton != null) {
            addButton.setOnAction(event -> addNewItem());
        }
        
        if (editButton != null) {
            editButton.setOnAction(event -> editSelectedItem());
        }
        
        if (deleteButton != null) {
            deleteButton.setOnAction(event -> deleteSelectedItems());
        }
        
        if (refreshButton != null) {
            refreshButton.setOnAction(event -> refreshData());
        }
        
        updateButtonStates();
    }
    
    /**
     * Sets up keyboard shortcuts for common management actions.
     */
    protected void setupKeyboardShortcuts() {
        // Wait for scene to be available
        Platform.runLater(() -> {
            if (tableView != null && tableView.getScene() != null) {
                javafx.scene.Scene scene = tableView.getScene();
                
                // Add common shortcuts: Ctrl+N (Add), Ctrl+E (Edit), Delete, F5 (Refresh)
                KeyboardAccessibilityHelper.addCommonShortcuts(scene,
                    this::addNewItem,
                    this::editSelectedItem,
                    this::deleteSelectedItems,
                    this::refreshData
                );
                
                logInfo("Keyboard shortcuts configured for %s", this.getClass().getSimpleName());
            }
        });
    }

    /**
     * Updates button enabled/disabled states based on selection.
     */
    protected void updateButtonStates() {
        boolean hasSelection = !tableView.getSelectionModel().getSelectedItems().isEmpty();
        boolean singleSelection = tableView.getSelectionModel().getSelectedItems().size() == 1;
        
        if (editButton != null) {
            editButton.setDisable(!singleSelection);
        }
        
        if (deleteButton != null) {
            deleteButton.setDisable(!hasSelection);
        }
    }

    /**
     * Updates the status label with current data count.
     */
    protected void updateStatusLabel() {
        if (statusLabel != null) {
            int totalCount = masterData.size();
            int filteredCount = filteredData.size();
            
            String status;
            if (totalCount == filteredCount) {
                status = getLocalizedMessage("status.items.total", totalCount);
            } else {
                status = getLocalizedMessage("status.items.filtered", filteredCount, totalCount);
            }
            
            statusLabel.setText(status);
        }
    }

    /**
     * Updates the table view placeholder based on whether there is data
     * and if search filters are active.
     */
    protected void updateTablePlaceholder() {
        if (tableView != null) {
            runOnFXThread(() -> {
                String searchText = searchField != null ? searchField.getText() : null;
                boolean isFiltering = searchText != null && !searchText.trim().isEmpty();
                
                Label placeholderLabel = new Label();
                placeholderLabel.getStyleClass().add("empty-table-placeholder");
                
                if (masterData.isEmpty()) {
                    placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items"));
                } else if (isFiltering && filteredData.isEmpty()) {
                    placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items.filtered"));
                } else {
                    placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items"));
                }
                
                tableView.setPlaceholder(placeholderLabel);
            });
        }
    }

    /**
     * Refreshes the data from the service layer.
     */
    protected void refreshData() {
        try {
            logInfo("Refreshing data");
            
            List<T> data = loadDataFromService();
            
            runOnFXThread(() -> {
                masterData.clear();
                masterData.addAll(data);
                updateStatusLabel();
                updateTablePlaceholder();
            });
            
            logInfo("Data refreshed successfully. Loaded %d items", data.size());
            
        } catch (Exception e) {
            handleException("refresh data", e);
        }
    }

    /**
     * Handles adding a new item.
     */
    protected void addNewItem() {
        try {
            logInfo("Adding new item");
            
            T newItem = showAddEditDialog(null);
            if (newItem != null) {
                T savedItem = saveItem(newItem);
                if (savedItem != null) {
                    masterData.add(savedItem);
                    tableView.getSelectionModel().select(savedItem);
                    updateStatusLabel();
                    updateTablePlaceholder();
                    
                    showInfo(
                        getLocalizedMessage("success.title"),
                        getLocalizedMessage("success.item.added")
                    );
                }
            }
            
        } catch (Exception e) {
            handleException("add new item", e);
        }
    }

    /**
     * Handles editing the selected item.
     */
    protected void editSelectedItem() {
        T selectedItem = tableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        
        try {
            logInfo("Editing item: %s", selectedItem);
            
            T editedItem = showAddEditDialog(selectedItem);
            if (editedItem != null) {
                T savedItem = saveItem(editedItem);
                if (savedItem != null) {
                    // Update the item in the list
                    int index = masterData.indexOf(selectedItem);
                    if (index >= 0) {
                        masterData.set(index, savedItem);
                    }
                    
                    showInfo(
                        getLocalizedMessage("success.title"),
                        getLocalizedMessage("success.item.updated")
                    );
                }
            }
            
        } catch (Exception e) {
            handleException("edit item", e);
        }
    }

    /**
     * Handles deleting selected items.
     */
    protected void deleteSelectedItems() {
        List<T> selectedItems = tableView.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            return;
        }
        
        executeWithExceptionHandling("delete items", () -> {
            boolean confirmed;
            
            if (selectedItems.size() == 1) {
                T item = selectedItems.get(0);
                String entityType = getEntityTypeName();
                String entityName = getEntityDisplayName(item);
                confirmed = alertManager.showDeleteConfirmation(entityType, entityName);
            } else {
                String confirmMessage = i18nManager.getMessage("confirm.delete.multiple", selectedItems.size());
                confirmed = alertManager.showConfirmation(i18nManager.getMessage("confirm.delete.title"), confirmMessage);
            }
            
            if (confirmed) {
                logInfo("Deleting %d items", selectedItems.size());
                
                for (T item : selectedItems) {
                    deleteItem(item);
                }
                
                masterData.removeAll(selectedItems);
                updateStatusLabel();
                updateTablePlaceholder();
                
                alertManager.showSuccess("success.items.deleted", selectedItems.size());
            }
        });
    }

    /**
     * Gets the entity type name for delete confirmation dialogs.
     * Should be overridden by subclasses to provide localized entity type.
     */
    protected abstract String getEntityTypeName();

    /**
     * Gets the display name for an entity for delete confirmation dialogs.
     * Should be overridden by subclasses to provide meaningful entity identification.
     */
    protected abstract String getEntityDisplayName(T entity);

    // Abstract methods to be implemented by subclasses

    /**
     * Sets up table columns specific to the entity type.
     */
    protected abstract void setupTableColumns();

    /**
     * Creates a search predicate for filtering based on the search text.
     */
    protected abstract Predicate<T> createSearchPredicate(String searchText);

    /**
     * Loads data from the service layer.
     */
    protected abstract List<T> loadDataFromService();

    /**
     * Shows the add/edit dialog for the entity.
     * 
     * @param item The item to edit, or null for adding new item
     * @return The modified item, or null if cancelled
     */
    protected abstract T showAddEditDialog(T item);

    /**
     * Saves an item using the service layer.
     */
    protected abstract T saveItem(T item);

    /**
     * Deletes an item using the service layer.
     */
    protected abstract void deleteItem(T item);

    // Utility methods for common table operations

    /**
     * Creates a table column with the specified property and title.
     */
    protected <S> TableColumn<T, S> createColumn(String title, String property, double prefWidth) {
        TableColumn<T, S> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        column.setPrefWidth(prefWidth);
        return column;
    }

    /**
     * Creates a table column with custom cell factory.
     */
    protected <S> TableColumn<T, S> createColumn(String title, String property, double prefWidth, 
                                               javafx.util.Callback<TableColumn<T, S>, TableCell<T, S>> cellFactory) {
        TableColumn<T, S> column = createColumn(title, property, prefWidth);
        column.setCellFactory(cellFactory);
        return column;
    }

    /**
     * Gets the currently selected item.
     */
    protected T getSelectedItem() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    /**
     * Gets all selected items.
     */
    protected List<T> getSelectedItems() {
        return tableView.getSelectionModel().getSelectedItems();
    }

    /**
     * Selects an item in the table.
     */
    protected void selectItem(T item) {
        tableView.getSelectionModel().select(item);
    }

    /**
     * Clears the table selection.
     */
    protected void clearSelection() {
        tableView.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    /**
     * Initialises pagination state and renders the pagination bar.
     * Call this from subclass initialize() after data is loaded.
     */
    protected void setupPagination() {
        recalculateTotalPages();
        updatePaginationBar();
    }

    /**
     * Recalculates totalPages based on current filteredData size and pageSize.
     */
    private void recalculateTotalPages() {
        int size = (filteredData != null) ? filteredData.size() : 0;
        int ps = (pageSize > 0) ? pageSize : 15;
        totalPages = (size == 0) ? 1 : (int) Math.ceil((double) size / ps);
        // Reset currentPage if it exceeds totalPages
        if (currentPage > totalPages) {
            currentPage = 1;
        }
    }

    /**
     * Rebuilds the pagination bar: previous/next button states + page buttons.
     */
    protected void updatePaginationBar() {
        recalculateTotalPages();

        if (previousButton != null) {
            previousButton.setDisable(currentPage == 1);
        }
        if (nextButton != null) {
            nextButton.setDisable(currentPage == totalPages);
        }

        buildPageButtons();
    }

    /**
     * Populates pageButtonsContainer with numbered page buttons and ellipsis labels.
     */
    protected void buildPageButtons() {
        if (pageButtonsContainer == null) return;

        pageButtonsContainer.getChildren().clear();

        List<Integer> visiblePages = PaginationHelper.buildVisiblePages(currentPage, totalPages);

        int prev = 0;
        for (int page : visiblePages) {
            // Insert ellipsis label for gaps
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

    /**
     * Navigates to the given page and refreshes the table.
     *
     * @param page target page (1-based)
     */
    protected void goToPage(int page) {
        if (page < 1 || page > totalPages) return;
        currentPage = page;
        updatePaginationBar();
        refreshTableForCurrentPage();
    }

    /**
     * Returns the slice of filteredData for the current page.
     */
    protected List<T> getCurrentPageData() {
        if (filteredData == null) return List.of();
        int ps = (pageSize > 0) ? pageSize : 15;
        int fromIndex = (currentPage - 1) * ps;
        int toIndex = Math.min(fromIndex + ps, filteredData.size());
        if (fromIndex >= filteredData.size()) return List.of();
        return filteredData.subList(fromIndex, toIndex);
    }

    /**
     * Refreshes the tableView to show only the current page's data.
     * Subclasses may override for custom behaviour.
     */
    protected void refreshTableForCurrentPage() {
        if (tableView != null) {
            tableView.setItems(FXCollections.observableArrayList(getCurrentPageData()));
        }
    }

    // Pagination navigation handlers (wired via FXML or subclass)
    @FXML
    protected void handlePrevious() {
        goToPage(currentPage - 1);
    }

    @FXML
    protected void handleNext() {
        goToPage(currentPage + 1);
    }
}