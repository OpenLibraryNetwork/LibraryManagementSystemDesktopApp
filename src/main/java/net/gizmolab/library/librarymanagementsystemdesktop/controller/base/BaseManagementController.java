package net.gizmolab.library.librarymanagementsystemdesktop.controller.base;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.StrapiPageResponse;
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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

/**
 * Base controller for management views that display data in tables with CRUD operations.
 * Provides common functionality for search, filtering, and table management.
 *
 * Supports two modes:
 * - **Server-side pagination** (for Strapi entities): Override {@link #loadPageFromService(int, int, String)}
 *   to return a {@link StrapiPageResponse}. Search and pagination are delegated to the server.
 * - **Client-side pagination** (for local H2 entities): Override {@link #loadDataFromService()}
 *   to return all data. Search is filtered client-side via {@link #createSearchPredicate(String)}.
 *
 * The mode is determined by {@link #isServerSidePagination()} — override it and return true to opt in.
 *
 * @param <T> The type of entity being managed
 */
public abstract class BaseManagementController<T> extends BaseController implements Initializable {


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
    protected int totalItems = 0;

    // Search debounce
    private Timer searchDebounceTimer;
    private static final long SEARCH_DEBOUNCE_MS = 300;
    protected String currentSearchQuery = null;

    // Data management — used in client-side mode
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

    // ═══════════════════════════════════════════════════════
    // Mode selection
    // ═══════════════════════════════════════════════════════

    /**
     * Override and return true to enable server-side pagination.
     * When true, the controller uses {@link #loadPageFromService(int, int, String)}
     * instead of {@link #loadDataFromService()}.
     */
    protected boolean isServerSidePagination() {
        return false;
    }

    // ═══════════════════════════════════════════════════════
    // Table setup
    // ═══════════════════════════════════════════════════════

    /** Message of the last failed server-side page load, or null. Shown as the table placeholder. */
    private String lastLoadError;

    /**
     * Sets up the table view with columns and selection behavior.
     */
    protected void setupTableView() {
        // Setup table columns - to be implemented by subclasses
        setupTableColumns();

        if (isServerSidePagination()) {
            // Server-side mode: table items set directly per page
            tableView.setItems(FXCollections.observableArrayList());
        } else {
            // Client-side mode: filtered + sorted lists
            filteredData = new FilteredList<>(masterData, p -> true);
            sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(tableView.comparatorProperty());
            tableView.setItems(sortedData);
        }

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

    // ═══════════════════════════════════════════════════════
    // Search functionality
    // ═══════════════════════════════════════════════════════

    /**
     * Sets up search functionality — debounced server-side or immediate client-side.
     */
    protected void setupSearchFunctionality() {
        if (searchField != null) {
            if (isServerSidePagination()) {
                // Server-side: debounced search with API call
                searchField.textProperty().addListener((observable, oldValue, newValue) -> scheduleServerSearch(newValue));
            } else {
                // Client-side: immediate filtering
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    filteredData.setPredicate(createSearchPredicate(newValue));
                    updateStatusLabel();
                    updateTablePlaceholder();
                });
            }
        }
    }

    /**
     * Schedules a server-side search after a debounce delay.
     * Cancels any pending search if the user types again within the delay.
     */
    private void scheduleServerSearch(String query) {
        if (searchDebounceTimer != null) {
            searchDebounceTimer.cancel();
        }
        searchDebounceTimer = new Timer(true); // daemon
        searchDebounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    currentSearchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
                    currentPage = 1; // reset to first page on new search
                    refreshData();
                });
            }
        }, SEARCH_DEBOUNCE_MS);
    }

    // ═══════════════════════════════════════════════════════
    // Button setup
    // ═══════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════
    // Status & placeholder
    // ═══════════════════════════════════════════════════════

    /**
     * Updates the status label with current data count.
     */
    protected void updateStatusLabel() {
        if (statusLabel != null) {
            if (isServerSidePagination()) {
                String status = getLocalizedMessage("status.items.total", totalItems);
                if (currentSearchQuery != null) {
                    status = getLocalizedMessage("status.items.filtered", tableView.getItems().size(), totalItems);
                }
                statusLabel.setText(status);
            } else {
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

                if (isServerSidePagination() && lastLoadError != null) {
                    // A failed load (403, server down, …) must not look like "no items"
                    placeholderLabel.setText(lastLoadError);
                } else if (isServerSidePagination()) {
                    if (totalItems == 0 && !isFiltering) {
                        placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items"));
                    } else if (tableView.getItems().isEmpty() && isFiltering) {
                        placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items.filtered"));
                    } else {
                        placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items"));
                    }
                } else {
                    if (masterData.isEmpty()) {
                        placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items"));
                    } else if (isFiltering && filteredData.isEmpty()) {
                        placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items.filtered"));
                    } else {
                        placeholderLabel.setText(getLocalizedMessage("table.placeholder.no.items"));
                    }
                }

                tableView.setPlaceholder(placeholderLabel);
            });
        }
    }

    // ═══════════════════════════════════════════════════════
    // Data loading — dual mode
    // ═══════════════════════════════════════════════════════

    /**
     * Refreshes the data. Delegates to server-side or client-side loading
     * depending on the mode.
     */
    protected void refreshData() {
        if (isServerSidePagination()) {
            refreshDataServerSide();
        } else {
            refreshDataClientSide();
        }
    }

    /**
     * Server-side pagination: loads a single page from the API.
     */
    private void refreshDataServerSide() {
        try {
            logInfo("Loading page %d (server-side)", currentPage);

            StrapiPageResponse<T> pageResponse = loadPageFromService(currentPage, pageSize, currentSearchQuery);

            runOnFXThread(() -> {
                totalPages = pageResponse.getPageCount();
                totalItems = pageResponse.getTotal();
                lastLoadError = pageResponse.getErrorMessage();

                tableView.setItems(FXCollections.observableArrayList(pageResponse.getData()));
                updatePaginationBar();
                updateStatusLabel();
                updateTablePlaceholder();
            });

            logInfo("Server page loaded: %d items (page %d/%d, total %d)",
                    pageResponse.getData().size(), currentPage, totalPages, totalItems);

        } catch (Exception e) {
            handleException("load page", e);
        }
    }

    /**
     * Client-side pagination: loads all data and filters locally.
     */
    private void refreshDataClientSide() {
        try {
            logInfo("Refreshing data (client-side)");

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

    // ═══════════════════════════════════════════════════════
    // CRUD operations
    // ═══════════════════════════════════════════════════════

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
                    if (isServerSidePagination()) {
                        // Reload current page to show changes
                        refreshData();
                    } else {
                        masterData.add(savedItem);
                        tableView.getSelectionModel().select(savedItem);
                        updateStatusLabel();
                        updateTablePlaceholder();
                    }

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
            logInfo("Editing an item"); // not the item itself: a borrower's toString is their name

            T editedItem = showAddEditDialog(selectedItem);
            if (editedItem != null) {
                T savedItem = saveItem(editedItem);
                if (savedItem != null) {
                    if (isServerSidePagination()) {
                        // Reload current page to show changes
                        refreshData();
                    } else {
                        // Update the item in the list
                        int index = masterData.indexOf(selectedItem);
                        if (index >= 0) {
                            masterData.set(index, savedItem);
                        }
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

                if (isServerSidePagination()) {
                    // Reload current page (may need to go back a page if last items deleted)
                    refreshData();
                } else {
                    masterData.removeAll(selectedItems);
                    updateStatusLabel();
                    updateTablePlaceholder();
                }

                alertManager.showSuccess("success.items.deleted", selectedItems.size());
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    // Abstract methods — entity identity
    // ═══════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════
    // Abstract methods — data loading
    // ═══════════════════════════════════════════════════════

    /**
     * Sets up table columns specific to the entity type.
     */
    protected abstract void setupTableColumns();

    /**
     * Creates a search predicate for client-side filtering based on the search text.
     * Only used when {@link #isServerSidePagination()} returns false.
     */
    protected abstract Predicate<T> createSearchPredicate(String searchText);

    /**
     * Loads all data from the service layer (client-side mode).
     * Override this for local H2 entities (Users, Borrows).
     */
    protected List<T> loadDataFromService() {
        return List.of(); // Default: no data
    }

    /**
     * Loads a single page from the server (server-side pagination mode).
     * Override this for Strapi entities (Publications, Authors, Publishers, Magazines).
     *
     * @param page        1-based page number
     * @param pageSize    items per page
     * @param searchQuery search text, or null for no filter
     * @return a page response with data and pagination metadata
     */
    protected StrapiPageResponse<T> loadPageFromService(int page, int pageSize, String searchQuery) {
        return StrapiPageResponse.empty(); // Default: empty page
    }

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

    // ═══════════════════════════════════════════════════════
    // Table utilities
    // ═══════════════════════════════════════════════════════

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
     * Recalculates totalPages based on current data size and pageSize.
     * Only used in client-side mode.
     */
    private void recalculateTotalPages() {
        if (isServerSidePagination()) {
            // Server-side: totalPages is set by the server response
            return;
        }
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
        if (!isServerSidePagination()) {
            recalculateTotalPages();
        }

        if (previousButton != null) {
            previousButton.setDisable(currentPage == 1);
        }
        if (nextButton != null) {
            nextButton.setDisable(currentPage >= totalPages);
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

        if (isServerSidePagination()) {
            // Server-side: fetch the new page from API
            refreshData();
        } else {
            updatePaginationBar();
            refreshTableForCurrentPage();
        }
    }

    /**
     * Returns the slice of filteredData for the current page (client-side mode only).
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
     * Refreshes the tableView to show only the current page's data (client-side mode).
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