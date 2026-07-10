package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.UserDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.I18nManager;
import net.gizmolab.library.librarymanagementsystemdesktop.service.exceptions.EntityNotFoundException;
import net.gizmolab.library.librarymanagementsystemdesktop.util.StylesheetHelper;
import net.gizmolab.library.librarymanagementsystemdesktop.util.TableCellFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class UserManagementController extends BaseManagementController<UserDTO> {

    @Autowired
    private IUserService userService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private FXMLLoaderFactory fxmlLoaderFactory;

    // FXML Components specific to user management
    @FXML private Label titleLabel;
    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, Long> idColumn;
    @FXML private TableColumn<UserDTO, String> firstNameColumn;
    @FXML private TableColumn<UserDTO, String> lastNameColumn;
    @FXML private TableColumn<UserDTO, String> emailColumn;
    @FXML private TableColumn<UserDTO, String> phoneColumn;
    @FXML private TableColumn<UserDTO, Integer> activeBorrowsColumn;
    @FXML private Label tableStatusLabel;
    @FXML private Button borrowHistoryButton;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        // Set the tableView reference for the base class
        this.tableView = usersTable;
        
        // Call parent initialization
        super.initialize(location, resources);
        
        // Setup pagination (uses base class fields)
        setupPagination();
        
        // Update UI with current language
        updateUI();
    }

    @Override
    protected void setupTableColumns() {
        // ID Column - numeric with right alignment
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        idColumn.setCellFactory(TableCellFactory.createNumericCellFactory());

        // First Name Column - text with ellipsis and tooltip
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        firstNameColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Last Name Column - text with ellipsis and tooltip
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastname"));
        lastNameColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Email Column - text with ellipsis and tooltip
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Phone Column - text with ellipsis and tooltip
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneColumn.setCellFactory(TableCellFactory.createTextCellFactory());

        // Active Borrows Column - numeric with right alignment
        activeBorrowsColumn.setCellValueFactory(new PropertyValueFactory<>("activeBorrowCount"));
        activeBorrowsColumn.setCellFactory(TableCellFactory.createNumericCellFactory());

        // Set table placeholder
        usersTable.setPlaceholder(new Label(i18nManager.getMessage("status.loading.users")));
    }

    @Override
    protected Predicate<UserDTO> createSearchPredicate(String searchText) {
        return user -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }

            String lowerCaseFilter = searchText.toLowerCase();

            // Search in first name
            if (user.getFirstname() != null && user.getFirstname().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            // Search in last name
            if (user.getLastname() != null && user.getLastname().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            // Search in full name
            if (user.getFullName().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            // Search in email
            if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            // Search in phone
            if (user.getPhone() != null && user.getPhone().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }

            return false;
        };
    }

    @Override
    protected List<UserDTO> loadDataFromService() {
        List<User> users = userService.getAllUsersWithActiveBorrows();
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    protected UserDTO showAddEditDialog(UserDTO user) {
        try {
            // Use Spring's FXMLLoaderFactory for proper dependency injection and i18n
            FXMLLoaderFactory fxmlLoaderFactory =
                getApplicationContext().getBean(FXMLLoaderFactory.class);
            
            var loadResult = fxmlLoaderFactory.<javafx.scene.Parent, UserFormController>loadWithController("/fxml/user-form-dialog.fxml");
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle(user == null ? 
                i18nManager.getMessage("user.addUser") : 
                i18nManager.getMessage("user.editUser"));
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(usersTable.getScene().getWindow());
            
            Scene dialogScene = new Scene(loadResult.getRoot());
            
            // Apply modern CSS theme to dialog
            StylesheetHelper.applyModernTheme(dialogScene);
            
            dialogStage.setScene(dialogScene);
            dialogStage.setResizable(false);
            dialogStage.sizeToScene();

            UserFormController controller = loadResult.getController();
            controller.setDialogStage(dialogStage);
            controller.setUser(user);

            // Create a holder for the result
            final UserDTO[] result = {null};
            controller.setOnSaveCallback(savedUser -> result[0] = savedUser);

            dialogStage.showAndWait();
            return result[0];

        } catch (Exception e) {
            logError("Failed to open user dialog", e);
            showError(i18nManager.getMessage("error.title"), 
                     "Failed to open user dialog: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected UserDTO saveItem(UserDTO userDTO) {
        try {
            User user = convertToEntity(userDTO);
            User savedUser;
            
            if (userDTO.getUserId() != null) {
                savedUser = userService.updateUser(userDTO.getUserId(), user);
            } else {
                savedUser = userService.createUser(user);
            }
            
            return convertToDTO(savedUser);
        } catch (EntityNotFoundException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    @Override
    protected void deleteItem(UserDTO userDTO) {
        try {
            userService.deleteUser(userDTO.getUserId());
        } catch (EntityNotFoundException e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    // Custom event handlers for pagination (not implemented in base class)
    @FXML
    private void handleAddUser() {
        addNewItem();
    }

    @FXML
    private void handleEditUser() {
        editSelectedItem();
    }

    @FXML
    private void handleDeleteUser() {
        deleteSelectedItems();
    }

    @FXML
    private void handleBorrowHistory() {
        UserDTO selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }

        try {
            FXMLLoaderFactory.LoadResult<Parent, BorrowHistoryModalController> loadResult =
                fxmlLoaderFactory.loadWithController("/fxml/borrow-history-modal.fxml");

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initOwner(usersTable.getScene().getWindow());
            modalStage.setTitle(i18nManager.getMessage("borrowHistory.title", selectedUser.getFullName()));
            modalStage.setResizable(true);

            Scene scene = new Scene(loadResult.getRoot(), 800, 600);
            StylesheetHelper.applyModernTheme(scene);
            modalStage.setScene(scene);

            modalStage.setMinWidth(600);
            modalStage.setMinHeight(400);

            BorrowHistoryModalController controller = loadResult.getController();
            User userEntity = userService.getUserById(selectedUser.getUserId());
            controller.setUser(userEntity);
            controller.setDialogStage(modalStage);

            modalStage.showAndWait();
        } catch (Exception e) {
            showError(i18nManager.getMessage("error.title"), i18nManager.getMessage("borrowHistory.error.open"));
        }
    }

    @Override
    protected void updateButtonStates() {
        super.updateButtonStates();
        boolean singleSelection = usersTable.getSelectionModel().getSelectedItems().size() == 1;
        if (borrowHistoryButton != null) {
            borrowHistoryButton.setDisable(!singleSelection);
        }
    }



    public void updateUI() {
        Platform.runLater(() -> {
            if (titleLabel != null) {
                titleLabel.setText(i18nManager.getMessage("user.userList"));
            }
            if (addButton != null) {
                addButton.setText(i18nManager.getMessage("user.addUser"));
            }
            if (editButton != null) {
                editButton.setText(i18nManager.getMessage("common.edit"));
            }
            if (deleteButton != null) {
                deleteButton.setText(i18nManager.getMessage("common.delete"));
            }
            if (searchField != null) {
                searchField.setPromptText(i18nManager.getMessage("common.search"));
            }
            
            // Update table column headers
            if (firstNameColumn != null) {
                firstNameColumn.setText(i18nManager.getMessage("user.firstName"));
            }
            if (lastNameColumn != null) {
                lastNameColumn.setText(i18nManager.getMessage("user.lastName"));
            }
            if (emailColumn != null) {
                emailColumn.setText(i18nManager.getMessage("user.email"));
            }
            if (phoneColumn != null) {
                phoneColumn.setText(i18nManager.getMessage("user.phone"));
            }
            if (activeBorrowsColumn != null) {
                activeBorrowsColumn.setText(i18nManager.getMessage("user.activeBorrows"));
            }
            
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
        return i18nManager.getMessage("user.firstName");
    }

    @Override
    protected String getEntityDisplayName(UserDTO user) {
        return user.getFirstname() + " " + user.getLastname();
    }

    // Helper methods for entity conversion
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
            user.getUserId(),
            user.getFirstname(),
            user.getLastname(),
            user.getEmail(),
            user.getPhone(),
            user.getActiveBorrowCount()
        );
    }

    private User convertToEntity(UserDTO userDTO) {
        User user = new User();
        user.setUserId(userDTO.getUserId());
        user.setFirstname(userDTO.getFirstname());
        user.setLastname(userDTO.getLastname());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        return user;
    }
}