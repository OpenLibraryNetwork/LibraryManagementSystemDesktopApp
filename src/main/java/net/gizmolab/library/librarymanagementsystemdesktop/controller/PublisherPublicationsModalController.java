package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublisherDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PaginationHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.TableCellFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.util.UserMessages;
import tools.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Controller for the modal dialog displaying publications associated with a specific publisher.
 * Uses StrapiApiClient to load publications directly from Strapi.
 */
@Component
@Scope("prototype")
public class PublisherPublicationsModalController {

    private static final Logger logger = LoggerFactory.getLogger(PublisherPublicationsModalController.class);

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private I18nManager i18nManager;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;

    // FXML components
    @FXML private TableView<PublicationDTO> publicationsTable;
    @FXML private TableColumn<PublicationDTO, String> typeColumn;
    @FXML private TableColumn<PublicationDTO, String> titleColumn;
    @FXML private TableColumn<PublicationDTO, String> authorColumn;
    @FXML private TableColumn<PublicationDTO, String> yearColumn;
    @FXML private TableColumn<PublicationDTO, String> pagesColumn;
    @FXML private TextField searchField;
    @FXML private Button closeButton;
    @FXML private HBox pageButtonsContainer;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Label tableStatusLabel;

    // Data management
    private ObservableList<PublicationDTO> masterData;
    private FilteredList<PublicationDTO> filteredData;
    private SortedList<PublicationDTO> sortedData;

    // Pagination state
    private int currentPage = 1;
    private static final int PAGE_SIZE = 15;
    private int totalPages = 1;

    // Context
    private String publisherId; // documentId
    private Stage dialogStage;

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
        loadData();
    }

    public void setPublisher(PublisherDTO publisher) {
        if (publisher != null) {
            this.publisherId = publisher.getDocumentId();
            loadData();
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    @FXML
    private void initialize() {
        masterData = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(publicationsTable.comparatorProperty());

        setupTableColumns();
        setupSearchFunctionality();
        setupPagination();

        publicationsTable.setRowFactory(tv -> {
            TableRow<PublicationDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openPublication(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupTableColumns() {
        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType()));
        typeColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle()));
        titleColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        authorColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAuthorNames()));
        authorColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        yearColumn.setCellValueFactory(cellData -> {
            Integer year = cellData.getValue().getYearPublished();
            return new SimpleStringProperty(year != null ? String.valueOf(year) : "");
        });
        yearColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        pagesColumn.setCellValueFactory(cellData -> {
            Integer pages = cellData.getValue().getPages();
            return new SimpleStringProperty(pages != null ? String.valueOf(pages) : "");
        });
        pagesColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        publicationsTable.setPlaceholder(new Label(i18nManager.getMessage("common.loading")));
    }

    private void setupSearchFunctionality() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        }
    }

    private void applyFilters() {
        filteredData.setPredicate(createSearchPredicate());
        currentPage = 1;
        totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PAGE_SIZE));
        refreshTableForCurrentPage();
    }

    private Predicate<PublicationDTO> createSearchPredicate() {
        String searchText = searchField != null ? searchField.getText() : null;

        return pub -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = searchText.toLowerCase();

            if (pub.getTitle() != null && pub.getTitle().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            if (pub.getAuthorNames() != null && pub.getAuthorNames().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            return pub.getType() != null && pub.getType().toLowerCase().contains(lowerCaseFilter);
        };
    }

    private void loadData() {
        publicationsTable.setPlaceholder(new Label(i18nManager.getMessage("common.loading")));

        Task<List<PublicationDTO>> loadTask = new Task<>() {
            @Override
            protected List<PublicationDTO> call() throws Exception {
                String libId = net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService.getCurrentLibraryDocumentId();
                JsonNode response = strapiApiClient.getPublisherBooksInLibrary(publisherId, libId);
                return DTOConverter.publicationsFromJson(response);
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<PublicationDTO> pubs = loadTask.getValue();
            masterData.setAll(pubs);

            if (pubs.isEmpty()) {
                publicationsTable.setPlaceholder(new Label(i18nManager.getMessage("publisherPublications.noPublications")));
            }

            totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PAGE_SIZE));
            currentPage = 1;
            refreshTableForCurrentPage();
        });

        loadTask.setOnFailed(event -> {
            Throwable ex = loadTask.getException();
            logger.error("Failed to load publications for publisher: {}", publisherId, ex);
            publicationsTable.setPlaceholder(new Label(i18nManager.getMessage("publisherPublications.error.load")));
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setupPagination() {
        refreshTableForCurrentPage();
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

    private List<PublicationDTO> getCurrentPageData() {
        if (sortedData == null || sortedData.isEmpty()) return List.of();
        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, sortedData.size());
        if (fromIndex >= sortedData.size()) return List.of();
        return sortedData.subList(fromIndex, toIndex);
    }

    private void refreshTableForCurrentPage() {
        publicationsTable.setItems(FXCollections.observableArrayList(getCurrentPageData()));
        updatePaginationBar();
    }

    // FXML event handlers

    private void openPublication(PublicationDTO publication) {
        try {
            PublicationDetailWindow.open(fxmlLoaderFactory, publicationsTable.getScene().getWindow(), publication);
            loadData(); // copies may have changed
        } catch (Exception e) {
            logger.error("Failed to open publication {}", publication.getDocumentId(), e);
            new Alert(Alert.AlertType.ERROR, UserMessages.describe(e)).showAndWait();
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
