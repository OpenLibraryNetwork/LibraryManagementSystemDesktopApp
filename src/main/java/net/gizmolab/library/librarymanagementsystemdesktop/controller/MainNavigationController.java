package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseController;
import net.gizmolab.library.librarymanagementsystemdesktop.service.*;
import net.gizmolab.library.librarymanagementsystemdesktop.service.*;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.KeyboardAccessibilityHelper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Main navigation controller that manages the primary application interface.
 * Handles navigation between different modules, language switching, and login gate.
 */
@Component
public class MainNavigationController extends BaseController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainNavigationController.class);

    // FXML Components
    @FXML private Button languageButton;
    @FXML private Button dashboardButton;
    @FXML private Button publicationsButton;
    @FXML private Button brochuresButton;
    @FXML private Button usersButton;
    @FXML private Button borrowsButton;
    @FXML private Button authorsButton;
    @FXML private Button publishersButton;
    @FXML private Button magazinesButton;
    @FXML private Button logoutButton;
    @FXML private StackPane contentArea;
    @FXML private Label welcomeLabel;
    @FXML private Label welcomeMessageLabel;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;

    // Spring Services
    @Autowired private StrapiApiClient strapiApiClient;
    @Autowired private AuthService authService;
    @Autowired private IUserService userService;
    @Autowired private IBorrowService borrowService;
    @Autowired private FXMLLoaderFactory fxmlLoaderFactory;
    @Autowired private I18nManager i18nManager;

    // Current active button for styling
    private Button activeButton;
    private String currentModuleName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainNavigationController");

        if (!authService.isAuthenticated()) {
            showLoginScreen();
            return;
        }

        initializeMainUI();

        logger.info("MainNavigationController initialized successfully");
    }

    /** True once the sidebar, shortcuts and bindings are set up; a later login only refreshes state. */
    private boolean mainUiInitialized = false;

    private void initializeMainUI() {
        if (mainUiInitialized) {
            // Login after a logout: shortcuts and handlers already exist, do not register them twice
            updateConnectionStatus();
            Platform.runLater(this::onDashboardClicked);
            return;
        }
        mainUiInitialized = true;
        setupLanguageSelector();
        setupNavigationButtons();
        updateConnectionStatus();
        setupI18nBindings();
        setupKeyboardShortcuts();

        // Set initial status
        updateStatus("status.ready");

        // Load the dashboard as the initial view
        Platform.runLater(this::onDashboardClicked);
    }

    /**
     * Sidebar "Αποσύνδεση": clears the session (memory + OS keystore) and shows the login screen.
     */
    @FXML
    private void onLogoutClicked() {
        if (!showConfirmation(getLocalizedMessage("navigation.logout"), getLocalizedMessage("logout.confirm"))) {
            return;
        }
        authService.logout();
        updateConnectionStatus();
        showLoginScreen();
    }

    private void showLoginScreen() {
        try {
            var result = fxmlLoaderFactory.<Node, LoginController>loadWithController("/fxml/login-screen.fxml");
            LoginController loginController = result.getController();
            loginController.setOnLoginSuccess(() -> {
                Platform.runLater(() -> {
                    contentArea.getChildren().clear();
                    initializeMainUI();
                });
            });
            contentArea.getChildren().clear();
            contentArea.getChildren().add(result.getRoot());
        } catch (Exception e) {
            logger.error("Failed to load login screen", e);
            handleException("Failed to load login screen", e);
        }
    }

    /**
     * Sets up the language selector with available languages.
     */
    private void setupLanguageSelector() {
        // Set FontAwesome globe icon
        IconProvider icons = IconProvider.getInstance();
        languageButton.setGraphic(icons.getIcon("icon-globe", 16));

        ContextMenu languageMenu = new ContextMenu();

        Locale[] availableLocales = i18nManager.getAvailableLocales();
        Locale currentLocale = i18nManager.getCurrentLocale();

        for (Locale locale : availableLocales) {
            String displayName = i18nManager.getDisplayName(locale);
            MenuItem menuItem = new MenuItem(displayName);

            if (locale.equals(currentLocale)) {
                menuItem.setText("✓ " + displayName);
                menuItem.setStyle("-fx-font-weight: bold;");
            }

            menuItem.setOnAction(event -> {
                if (!locale.equals(i18nManager.getCurrentLocale())) {
                    changeLanguage(locale);
                    setupLanguageSelector();
                }
            });

            languageMenu.getItems().add(menuItem);
        }

        languageButton.setContextMenu(languageMenu);
    }

    @FXML
    private void onLanguageButtonClicked() {
        ContextMenu menu = languageButton.getContextMenu();
        if (menu != null) {
            menu.show(languageButton, javafx.geometry.Side.TOP, 0, 0);
        }
    }

    /**
     * Sets up navigation button styling and behavior.
     */
    private void setupNavigationButtons() {
        activeButton = null;

        // Set FontAwesome icons for navigation buttons (color managed by CSS .glyph-icon)
        IconProvider icons = IconProvider.getInstance();
        if (dashboardButton != null) dashboardButton.setGraphic(icons.getIcon("icon-dashboard", 16));
        if (publicationsButton != null) publicationsButton.setGraphic(icons.getIcon("icon-book", 16));
        if (brochuresButton != null) brochuresButton.setGraphic(icons.getIcon("icon-brochure", 16));
        if (usersButton != null) usersButton.setGraphic(icons.getIcon("icon-user", 16));
        if (borrowsButton != null) borrowsButton.setGraphic(icons.getIcon("icon-borrow", 16));
        if (authorsButton != null) authorsButton.setGraphic(icons.getIcon("icon-author", 16));
        if (publishersButton != null) publishersButton.setGraphic(icons.getIcon("icon-publisher", 16));
        if (magazinesButton != null) magazinesButton.setGraphic(icons.getIcon("icon-magazine", 16));

        Button[] navButtons = {dashboardButton, publicationsButton, brochuresButton, usersButton, borrowsButton, authorsButton, publishersButton, magazinesButton};

        for (Button button : navButtons) {
            if (button == null) continue;
            button.getStyleClass().addAll("sidebar-button", "nav-item");
        }
    }

    /**
     * Updates the connection status indicator.
     */
    private void updateConnectionStatus() {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.isOnline() && strapiApiClient.isServerReachable();
            }
        };
        task.setOnSucceeded(e -> {
            boolean online = task.getValue();
            Platform.runLater(() -> {
                connectionStatusLabel.setText(online ? "● Online" : "● Offline");
                connectionStatusLabel.getStyleClass().removeAll("status-online", "status-offline");
                connectionStatusLabel.getStyleClass().add(online ? "status-online" : "status-offline");
            });
        });
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                connectionStatusLabel.setText("● Offline");
                connectionStatusLabel.getStyleClass().removeAll("status-online", "status-offline");
                connectionStatusLabel.getStyleClass().add("status-offline");
            });
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // Navigation Event Handlers

    @FXML
    private void onDashboardClicked() {
        logger.info("Dashboard navigation clicked");
        setActiveButton(dashboardButton);
        loadModule("dashboard", "Dashboard");
    }

    @FXML
    private void onPublicationsClicked() {
        logger.info("Publications navigation clicked");
        setActiveButton(publicationsButton);
        loadModule("publications", "Publications Management");
    }

    @FXML
    private void onBrochuresClicked() {
        logger.info("Brochures navigation clicked");
        setActiveButton(brochuresButton);
        loadModule("brochures", "Brochures");
    }

    @FXML
    private void onUsersClicked() {
        logger.info("Users navigation clicked");
        setActiveButton(usersButton);
        loadModule("users", "Users Management");
    }

    @FXML
    private void onBorrowsClicked() {
        logger.info("Borrows navigation clicked");
        setActiveButton(borrowsButton);
        loadModule("borrows", "Borrows Management");
    }

    @FXML
    private void onAuthorsClicked() {
        logger.info("Authors navigation clicked");
        setActiveButton(authorsButton);
        loadModule("authors", "Authors Management");
    }

    @FXML
    private void onPublishersClicked() {
        logger.info("Publishers navigation clicked");
        setActiveButton(publishersButton);
        loadModule("publishers", "Publishers Management");
    }

    @FXML
    private void onMagazinesClicked() {
        logger.info("Magazines navigation clicked");
        setActiveButton(magazinesButton);
        loadModule("magazines", "Magazines Management");
    }

    /**
     * Sets the active navigation button and updates styling.
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().removeAll("sidebar-button-active", "nav-item-active");
        }
        activeButton = button;
        if (activeButton != null) {
            activeButton.getStyleClass().addAll("sidebar-button-active", "nav-item-active");
        }
    }

    /**
     * Loads a module view into the content area.
     */
    private void loadModule(String moduleName, String displayName) {
        this.currentModuleName = moduleName;
        try {
            Node moduleView = null;
            BaseController controller = null;

            switch (moduleName) {
                case "dashboard": {
                    var result = fxmlLoaderFactory.<Node, DashboardViewController>loadWithController("/fxml/dashboard-view.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                case "publications": {
                    var result = fxmlLoaderFactory.<Node, BookManagementController>loadWithController("/fxml/book-management.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                case "brochures": {
                    var result = fxmlLoaderFactory.<Node, BrochureManagementController>loadWithController("/fxml/brochure-management.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                case "users": {
                    var result = fxmlLoaderFactory.<Node, UserManagementController>loadWithController("/fxml/user-management.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                case "borrows": {
                    var result = fxmlLoaderFactory.<Node, BorrowManagementController>loadWithController("/fxml/borrow-management.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                case "authors": {
                    var result = fxmlLoaderFactory.<Node, AuthorManagementController>loadWithController("/fxml/author-management.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                case "publishers": {
                    var result = fxmlLoaderFactory.<Node, PublisherManagementController>loadWithController("/fxml/publisher-management.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                case "magazines": {
                    var result = fxmlLoaderFactory.<Node, MagazineManagementController>loadWithController("/fxml/magazine-management.fxml");
                    moduleView = result.getRoot();
                    controller = result.getController();
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown module: " + moduleName);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(moduleView);

            if (controller != null) {
                controller.setStatusUpdateCallback(this::updateStatus);
            }

        } catch (Exception e) {
            logger.error("Failed to load module: " + moduleName, e);
            showError("Error", "Failed to load " + displayName + ": " + e.getMessage());
            updateStatus("status.error");
        }
    }

    /**
     * Changes the application language.
     */
    private void changeLanguage(Locale locale) {
        logger.info("Changing language to: " + locale);
        try {
            i18nManager.setLocale(locale);
            updateStatus("status.language.changed");
        } catch (Exception e) {
            logger.error("Failed to change language to: " + locale, e);
            showError("Error", "Failed to change language: " + e.getMessage());
        }
    }

    /**
     * Updates the status bar message.
     */
    private void updateStatus(String messageKey, Object... args) {
        Platform.runLater(() -> {
            try {
                String message = i18nManager.getMessage(messageKey, args);
                statusLabel.setText(message);
            } catch (Exception e) {
                statusLabel.setText(messageKey);
            }
        });
    }

    @Override
    protected String getLocalizedMessage(String key, Object... args) {
        return i18nManager.getMessage(key, args);
    }

    /**
     * Sets up i18n bindings and listeners.
     */
    private void setupI18nBindings() {
        i18nManager.currentLocaleProperty().addListener((observable, oldLocale, newLocale) -> {
            Platform.runLater(this::refreshUI);
        });
    }

    /**
     * Sets up keyboard shortcuts for navigation and common actions.
     */
    private void setupKeyboardShortcuts() {
        Platform.runLater(() -> {
            if (contentArea != null && contentArea.getScene() != null) {
                javafx.scene.Scene scene = contentArea.getScene();

                KeyboardAccessibilityHelper.addNavigationShortcuts(scene,
                    this::onPublicationsClicked,
                    this::onUsersClicked,
                    this::onBorrowsClicked,
                    this::onAuthorsClicked,
                    this::onPublishersClicked
                );

                logger.info("Keyboard shortcuts configured successfully");
            }
        });
    }

    /**
     * Refreshes all UI elements with current language.
     */
    private void refreshUI() {
        try {
            Platform.runLater(() -> {
                if (welcomeLabel != null) {
                    welcomeLabel.setText(i18nManager.getMessage("welcome.title"));
                }
                if (welcomeMessageLabel != null) {
                    welcomeMessageLabel.setText(i18nManager.getMessage("welcome.message"));
                }

                if (dashboardButton != null) {
                    dashboardButton.setText(i18nManager.getMessage("navigation.dashboard"));
                }
                if (publicationsButton != null) {
                    publicationsButton.setText(i18nManager.getMessage("navigation.books"));
                }
                if (brochuresButton != null) {
                    brochuresButton.setText(i18nManager.getMessage("navigation.brochures"));
                }
                if (usersButton != null) {
                    usersButton.setText(i18nManager.getMessage("navigation.users"));
                }
                if (borrowsButton != null) {
                    borrowsButton.setText(i18nManager.getMessage("navigation.borrows"));
                }
                if (authorsButton != null) {
                    authorsButton.setText(i18nManager.getMessage("navigation.authors"));
                }
                if (publishersButton != null) {
                    publishersButton.setText(i18nManager.getMessage("navigation.publishers"));
                }
                if (magazinesButton != null) {
                    magazinesButton.setText(i18nManager.getMessage("navigation.magazines"));
                }
                if (logoutButton != null) {
                    logoutButton.setText(i18nManager.getMessage("navigation.logout"));
                }

                updateStatus("status.ready");
                updateConnectionStatus();

                if (currentModuleName != null) {
                    String displayKey = "navigation." + currentModuleName;
                    String displayNameStr = i18nManager.getMessage(displayKey);
                    loadModule(currentModuleName, displayNameStr);
                }
            });
        } catch (Exception e) {
            logger.error("Failed to refresh UI with new language", e);
        }
    }
}
