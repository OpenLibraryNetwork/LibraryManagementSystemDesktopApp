package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.CopyDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.service.*;
import net.gizmolab.library.librarymanagementsystemdesktop.service.AuthService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.StrapiApiClient;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import com.fasterxml.jackson.databind.JsonNode;
import net.gizmolab.library.librarymanagementsystemdesktop.util.PublicationDetailFormatter;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Borrow creation form.
 *
 * Flow:
 * 1. Select User (local H2 — ComboBox)
 * 2. Search Publication (Strapi — TextField + search button)
 * 3. Select from search results (ListView)
 * 4. Select Available Copy (Strapi — ComboBox, loaded when publication selected)
 * 5. Set Due Date (DatePicker, default +14 days)
 * 6. Save → IBorrowService.borrowItem(user, copy, pub, dueDate)
 */
@Controller
@Scope("prototype")
public class BorrowFormControllerNew extends BaseController {

    @Autowired private IUserService userService;
    @Autowired private IBorrowService borrowService;
    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private AuthService authService;

    // User selection (local H2)
    @FXML private ComboBox<User> userComboBox;

    // Publication search (Strapi)
    @FXML private TextField publicationSearchField;
    @FXML private Button searchPublicationButton;
    @FXML private ListView<PublicationDTO> publicationListView;

    // Copy selection (Strapi — loaded when publication selected)
    @FXML private ComboBox<CopyDTO> copyComboBox;

    // Due date
    @FXML private DatePicker dueDatePicker;

    // Info labels
    @FXML private Label selectedPublicationLabel;
    @FXML private Label selectedCopyLabel;
    @FXML private Label availableCopiesLabel;

    // Buttons
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // State
    private PublicationDTO selectedPublication;
    private Consumer<Void> onSaveCallback;

    public void initialize() {
        // Load users
        loadUsers();

        // Default due date: +14 days
        dueDatePicker.setValue(LocalDate.now().plusDays(14));

        // Publication search
        searchPublicationButton.setOnAction(e -> handleSearchPublication());
        publicationSearchField.setOnAction(e -> handleSearchPublication());

        // Publication selection → load copies
        publicationListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    selectedPublication = newVal;
                    selectedPublicationLabel.setText(PublicationDetailFormatter.displayTitle(newVal) +
                        PublicationDetailFormatter.authorSuffix(newVal));
                    loadAvailableCopies(newVal);
                }
            });

        // Copy selection display
        copyComboBox.setCellFactory(lv -> new ListCell<CopyDTO>() {
            @Override
            protected void updateItem(CopyDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" :
                    "Αντίτυπο #" + item.getCopyNumber() +
                    " (" + item.getCondition() + ")");
            }
        });
        copyComboBox.setButtonCell(copyComboBox.getCellFactory().call(null));

        // User display
        userComboBox.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" :
                    item.getFirstname() + " " + item.getLastname());
            }
        });
        userComboBox.setButtonCell(userComboBox.getCellFactory().call(null));

        // Publication list display
        publicationListView.setCellFactory(lv -> new ListCell<PublicationDTO>() {
            @Override
            protected void updateItem(PublicationDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(PublicationDetailFormatter.displayTitle(item) +
                        " [" + item.getType() + "]" +
                        PublicationDetailFormatter.authorSuffix(item) +
                        " (Διαθ: " + item.getAvailableCopies() + "/" + item.getTotalCopies() + ")");
                }
            }
        });

        saveButton.setOnAction(e -> handleSave());
        cancelButton.setOnAction(e -> handleCancel());
    }

    private void loadUsers() {
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                return userService.getAllUsers();
            }
        };
        task.setOnSucceeded(e -> {
            userComboBox.getItems().setAll(task.getValue());
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleSearchPublication() {
        String query = publicationSearchField.getText().trim();
        if (query.isEmpty()) return;

        searchPublicationButton.setDisable(true);

        Task<List<PublicationDTO>> task = new Task<>() {
            @Override
            protected List<PublicationDTO> call() throws Exception {
                JsonNode response = strapiApiClient.searchPublications(query);
                return DTOConverter.publicationsFromJson(response);
            }
        };

        task.setOnSucceeded(e -> {
            searchPublicationButton.setDisable(false);
            publicationListView.getItems().setAll(task.getValue());
            if (task.getValue().isEmpty()) {
                showInfo("Αναζήτηση", "Δεν βρέθηκαν αποτελέσματα για: " + query);
            }
        });

        task.setOnFailed(e -> {
            searchPublicationButton.setDisable(false);
            handleException("Search failed", (Exception) task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadAvailableCopies(PublicationDTO publication) {
        copyComboBox.getItems().clear();
        copyComboBox.setDisable(true);

        Task<List<CopyDTO>> task = new Task<>() {
            @Override
            protected List<CopyDTO> call() throws Exception {
                JsonNode response = strapiApiClient.getAvailableCopies(
                    publication.getId(), authService.getLibraryId());
                return DTOConverter.copiesFromJson(response);
            }
        };

        task.setOnSucceeded(e -> {
            List<CopyDTO> copies = task.getValue();
            copyComboBox.getItems().setAll(copies);
            copyComboBox.setDisable(false);
            if (availableCopiesLabel != null) {
                availableCopiesLabel.setText("Διαθέσιμα: " + copies.size());
            }

            if (copies.isEmpty()) {
                showWarning("Προσοχή", "Δεν υπάρχουν διαθέσιμα αντίτυπα");
            } else if (copies.size() == 1) {
                copyComboBox.getSelectionModel().selectFirst();
            }
        });

        task.setOnFailed(e -> {
            copyComboBox.setDisable(false);
            handleException("Failed to load copies", (Exception) task.getException());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleSave() {
        // Validate
        User user = userComboBox.getValue();
        CopyDTO copy = copyComboBox.getValue();

        if (user == null) { showWarning("Επιλογή", "Επιλέξτε χρήστη"); return; }
        if (selectedPublication == null) { showWarning("Επιλογή", "Αναζητήστε και επιλέξτε έντυπο"); return; }
        if (copy == null) { showWarning("Επιλογή", "Επιλέξτε αντίτυπο"); return; }
        if (dueDatePicker.getValue() == null) { showWarning("Επιλογή", "Επιλέξτε ημερομηνία επιστροφής"); return; }

        saveButton.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                borrowService.borrowItem(user, copy, selectedPublication, dueDatePicker.getValue());
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            saveButton.setDisable(false);
            showInfo("Επιτυχία", "Ο δανεισμός καταχωρήθηκε επιτυχώς!");
            if (onSaveCallback != null) onSaveCallback.accept(null);
            saveButton.getScene().getWindow().hide();
        });

        task.setOnFailed(e -> {
            saveButton.setDisable(false);
            Throwable ex = task.getException();
            if (ex instanceof StrapiApiClient.ConflictException ||
                (ex.getCause() != null && ex.getCause() instanceof StrapiApiClient.ConflictException)) {
                showWarning("Σύγκρουση", "Αυτό το αντίτυπο είναι ήδη δανεισμένο");
            } else if (ex instanceof StrapiApiClient.AuthenticationExpiredException ||
                (ex.getCause() != null && ex.getCause() instanceof StrapiApiClient.AuthenticationExpiredException)) {
                showWarning("Συνεδρία", "Η συνεδρία έληξε. Συνδεθείτε ξανά.");
            } else {
                handleException("Borrow failed", (Exception) ex);
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleCancel() {
        cancelButton.getScene().getWindow().hide();
    }

    public void setOnSaveCallback(Consumer<Void> callback) {
        this.onSaveCallback = callback;
    }
}
