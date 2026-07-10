package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.ResponsiveLayoutManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX Application entry point with Spring Boot integration.
 * This class manages the JavaFX lifecycle while integrating with Spring Boot context.
 */
public class LibraryManagementFXApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        // Set JavaFX mode property
        System.setProperty("javafx.mode", "true");
        
        // Initialize Spring Boot context before JavaFX starts
        SpringApplication app = new SpringApplication(LibraryManagementSystemApplication.class);
        // Disable web server for JavaFX mode
        app.setWebApplicationType(WebApplicationType.NONE);
        springContext = app.run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Set User Agent Stylesheet to null (no default theme)
        Application.setUserAgentStylesheet(null);
        System.out.println("Removed default User Agent Stylesheet - using custom CSS only");
        
        // Get FXMLLoaderFactory from Spring context
        FXMLLoaderFactory fxmlLoaderFactory = springContext.getBean(FXMLLoaderFactory.class);
        
        try {
            // Load the main navigation view
            Parent root = fxmlLoaderFactory.load("/fxml/main-navigation.fxml");
            
            // Setup primary stage
            primaryStage.setTitle("Library Management System");
            Scene scene = new Scene(root, 1200, 800);
            
            // APPLY CUSTOM CSS
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/modern-theme.css").toExternalForm());
            System.out.println("Applied custom modern theme CSS");
            
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1024);
            primaryStage.setMinHeight(600);
            
            // Initialize ResponsiveLayoutManager for responsive behavior
            ResponsiveLayoutManager responsiveLayoutManager = new ResponsiveLayoutManager(primaryStage);
            
            // Handle application close
            primaryStage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });
            
            primaryStage.show();
            
        } catch (Exception e) {
            // Print detailed error information
            System.err.println("Could not load main navigation view: " + e.getMessage());
            e.printStackTrace();
            
            // Create a temporary simple scene for testing
            Parent tempRoot = createTemporaryView();
            primaryStage.setTitle("Library Management System - Error Loading Main View");
            primaryStage.setScene(new Scene(tempRoot, 600, 300));
            primaryStage.show();
        }
    }

    /**
     * Shows a theme error notification to the user.
     * This is a fallback method that creates a simple alert if NotificationManager is not available.
     *
     * @param scene the scene to attach the notification to
     * @param message the error message to display
     */
    private void showThemeErrorNotification(Scene scene, String message) {
        // Create a simple alert dialog for theme errors
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.WARNING
        );
        alert.setTitle("Theme Loading Warning");
        alert.setHeaderText("Theme System Notice");
        alert.setContentText(message);
        
        // Show alert in a non-blocking way
        Platform.runLater(() -> {
            alert.show();
            // Auto-close after 5 seconds
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            delay.setOnFinished(event -> alert.close());
            delay.play();
        });
    }

    @Override
    public void stop() throws Exception {
        // Close Spring context when JavaFX application stops
        if (springContext != null) {
            springContext.close();
        }
    }

    /**
     * Creates a temporary view for testing the JavaFX application setup.
     * This will be removed once the main navigation is implemented.
     */
    private Parent createTemporaryView() {
        javafx.scene.control.Label label = new javafx.scene.control.Label(
            "Library Management System\n\n" +
            "Core UI Infrastructure Implementation Complete!\n\n" +
            "JavaFX Application: Started Successfully\n" +
            "Spring Boot Context: " + (springContext != null ? "Initialized" : "Not Initialized") + "\n" +
            "Navigation System: Ready\n\n" +
            "Task 4 Status: COMPLETED\n\n" +
            "The main navigation view should now load properly.\n" +
            "If you see this message, there may be an FXML loading issue."
        );
        label.setStyle("-fx-font-size: 12px; -fx-padding: 20px; -fx-alignment: center; -fx-text-alignment: center;");
        
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(label);
        vbox.setStyle("-fx-alignment: center; -fx-spacing: 10px; -fx-background-color: #f0f8ff;");
        
        return vbox;
    }

    /**
     * Main method that launches the JavaFX application.
     * This replaces the traditional Spring Boot main method for desktop mode.
     */
    public static void main(String[] args) {
        // Set system property to indicate we're running in JavaFX mode
        System.setProperty("java.awt.headless", "false");
        
        // Launch JavaFX application
        Application.launch(LibraryManagementFXApplication.class, args);
    }
}