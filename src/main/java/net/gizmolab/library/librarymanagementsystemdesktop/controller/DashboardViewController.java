package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.application.Platform;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class DashboardViewController extends BaseController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(DashboardViewController.class);

    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private AuthService authService;
    @Autowired private IUserService userService;
    @Autowired private IBorrowService borrowService;

    @FXML private Label totalBooksValue;
    @FXML private Label activeLoansValue;
    @FXML private Label overdueValue;
    @FXML private Label registeredMembersValue;
    @FXML private VBox popularBooksContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadKpiData();
        loadPopularBooks();
    }

    private void loadKpiData() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Strapi: total publications
                long totalPubs = 0;
                try {
                    JsonNode response = strapiApiClient.get(
                        "/api/books?pagination[pageSize]=1&pagination[withCount]=true");
                    if (response != null && response.has("meta")) {
                        totalPubs = response.path("meta").path("pagination").path("total").asLong(0);
                    }
                } catch (Exception e) {
                    logger.warn("Could not fetch publication count from Strapi: {}", e.getMessage());
                }

                // H2: borrow stats
                Long activeBorrows = borrowService.countActiveBorrows();

                // H2: overdue count
                List<BorrowDTO> activeDTOs = borrowService.getActiveBorrowsAsDTO();
                long overdueCount = activeDTOs.stream()
                    .filter(BorrowDTO::isOverdue)
                    .count();

                // H2: user count
                Long userCount = userService.countUsers();

                final long pubs = totalPubs;
                final long borrows = activeBorrows != null ? activeBorrows : 0;
                final long overdue = overdueCount;
                final long users = userCount != null ? userCount : 0;

                Platform.runLater(() -> {
                    totalBooksValue.setText(String.valueOf(pubs));
                    activeLoansValue.setText(String.valueOf(borrows));
                    overdueValue.setText(String.valueOf(overdue));
                    registeredMembersValue.setText(String.valueOf(users));
                });

                return null;
            }
        };

        task.setOnFailed(e -> {
            logger.error("KPI loading failed", task.getException());
            Platform.runLater(() -> {
                totalBooksValue.setText("0");
                activeLoansValue.setText("0");
                overdueValue.setText("0");
                registeredMembersValue.setText("0");
            });
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadPopularBooks() {
        if (popularBooksContainer == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                List<BorrowDTO> allBorrows = borrowService.getAllBorrowsAsDTO();

                // Count borrows per publication title
                Map<String, Long> counts = allBorrows.stream()
                    .filter(b -> b.getPublicationTitle() != null)
                    .collect(Collectors.groupingBy(
                        BorrowDTO::getPublicationTitle,
                        Collectors.counting()));

                var topBooks = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .toList();

                Platform.runLater(() -> {
                    popularBooksContainer.getChildren().clear();
                    int rank = 1;
                    long maxCount = topBooks.isEmpty() ? 1 : topBooks.get(0).getValue();

                    for (var entry : topBooks) {
                        HBox row = createBookRow(entry.getKey(), entry.getValue(), rank, maxCount);
                        popularBooksContainer.getChildren().add(row);
                        rank++;
                    }
                });
                return null;
            }
        };

        task.setOnFailed(e -> logger.error("Failed to load popular books", task.getException()));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private HBox createBookRow(String title, long borrowCount, int rank, long maxCount) {
        HBox row = new HBox();
        row.getStyleClass().add("popular-book-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setSpacing(16.0);

        // 1. Rank Label
        Label rankLabel = new Label(String.format("%02d", rank));
        rankLabel.getStyleClass().add("popular-rank");

        // 2. Cover StackPane
        StackPane coverPane = new StackPane();
        coverPane.getStyleClass().addAll("popular-cover", "cover-gradient-" + (((rank - 1) % 10) + 1));

        // 3. Book Details VBox
        VBox detailsVBox = new VBox();
        detailsVBox.setSpacing(2.0);
        HBox.setHgrow(detailsVBox, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("popular-title");

        detailsVBox.getChildren().add(titleLabel);

        // 4. Progress Bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.getStyleClass().add("popular-progress-bar");
        if (rank % 3 == 0) {
            progressBar.getStyleClass().add("popular-progress-bar-teal");
        } else if (rank % 3 == 1) {
            progressBar.getStyleClass().add("popular-progress-bar-orange");
        } else {
            progressBar.getStyleClass().add("popular-progress-bar-purple");
        }
        double progress = maxCount > 0 ? (double) borrowCount / maxCount : 0.0;
        progressBar.setProgress(progress);

        // 5. Count
        Label countLabel = new Label(String.valueOf(borrowCount));
        countLabel.getStyleClass().add("popular-count");

        row.getChildren().addAll(rankLabel, coverPane, detailsVBox, progressBar, countLabel);
        return row;
    }
}
