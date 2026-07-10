package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.AuthorDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PaginationHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.TableCellFactory;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.List;
import java.util.function.Predicate;

/**
 * Controller for the modal dialog displaying publications associated with a specific author.
 */
@Component
@Scope("prototype")
public class AuthorBooksModalController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorBooksModalController.class);

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private I18nManager i18nManager;

    // FXML components
    @FXML private TableView<PublicationDTO> booksTable;
    @FXML private TableColumn<PublicationDTO, String> titleColumn;
    @FXML private TableColumn<PublicationDTO, String> isbnColumn;
    @FXML private TableColumn<PublicationDTO, String> publisherColumn;
    @FXML private TableColumn<PublicationDTO, Integer> pagesColumn;
    @FXML private TableColumn<PublicationDTO, Integer> yearColumn;
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
    private Long authorId;
    private Stage dialogStage;

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
        loadData();
    }

    public void setAuthor(AuthorDTO author) {
        if (author != null) {
            this.authorId = author.getId();
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
        sortedData.comparatorProperty().bind(booksTable.comparatorProperty());

        setupTableColumns();
        setupSearchFunctionality();
        setupPagination();
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        publisherColumn.setCellValueFactory(cellData -> {
            PublicationDTO pub = cellData.getValue();
            if (pub.getPublisher() != null) {
                return new SimpleStringProperty(pub.getPublisher().getName());
            }
            return new SimpleStringProperty("");
        });
        publisherColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        pagesColumn.setCellValueFactory(new PropertyValueFactory<>("pages"));
        pagesColumn.setCellFactory(TableCellFactory.createNumericCellFactory());

        yearColumn.setCellValueFactory(new PropertyValueFactory<>("yearPublished"));
        yearColumn.setCellFactory(TableCellFactory.createNumericCellFactory());

        booksTable.setPlaceholder(new Label(i18nManager.getMessage("common.loading")));
    }

    private void setupSearchFunctionality() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                applyFilters();
            });
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
            if (pub.getIsbn() != null && pub.getIsbn().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            if (pub.getPublisher() != null && pub.getPublisher().getName() != null
                    && pub.getPublisher().getName().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            return false;
        };
    }

    private void loadData() {
        booksTable.setPlaceholder(new Label(i18nManager.getMessage("common.loading")));

        Task<List<PublicationDTO>> loadTask = new Task<>() {
            @Override
            protected List<PublicationDTO> call() throws Exception {
                JsonNode response = strapiApiClient.get(
                    "/api/books?filters[authors][id][$eq]=" + authorId +
                    "&populate=authors,publisher,copies&pagination[pageSize]=100");
                return DTOConverter.publicationsFromJson(response);
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<PublicationDTO> dtos = loadTask.getValue();
            masterData.setAll(dtos);

            if (dtos.isEmpty()) {
                booksTable.setPlaceholder(new Label(i18nManager.getMessage("authorBooks.noBooks")));
            }

            totalPages = Math.max(1, (int) Math.ceil((double) filteredData.size() / PAGE_SIZE));
            currentPage = 1;
            refreshTableForCurrentPage();
        });

        loadTask.setOnFailed(event -> {
            Throwable ex = loadTask.getException();
            logger.error("Failed to load publications for author: {}", authorId, ex);
            booksTable.setPlaceholder(new Label(i18nManager.getMessage("authorBooks.error.load")));
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
        booksTable.setItems(FXCollections.observableArrayList(getCurrentPageData()));
        updatePaginationBar();
    }

    // FXML event handlers

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
