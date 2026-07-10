package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.service.theme.AtlantaFXTheme;
import net.gizmolab.library.librarymanagementsystemdesktop.service.theme.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify AtlantaFX theme is properly applied to the application.
 */
class AtlantaFXIntegrationTest {

    @Test
    void testAtlantaFXThemeIsApplied() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        // Launch JavaFX application in a separate thread
        new Thread(() -> {
            try {
                Application.launch(TestApp.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        // Wait for JavaFX to initialize
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "JavaFX should initialize within 10 seconds");
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            // Load theme BEFORE creating Scene
            ThemeManager.getInstance().loadSavedTheme();
            
            // Create a simple scene
            Button button = new Button("Test Button");
            button.getStyleClass().add("button-primary");
            
            VBox root = new VBox(button);
            Scene scene = new Scene(root, 400, 300);
            
            // Initialize ThemeManager with scene
            ThemeManager.getInstance().initialize(scene);
            
            // Verify User Agent Stylesheet is set
            String userAgentStylesheet = Application.getUserAgentStylesheet();
            System.out.println("User Agent Stylesheet: " + userAgentStylesheet);
            
            assertNotNull(userAgentStylesheet, "User Agent Stylesheet should be set");
            assertTrue(userAgentStylesheet.contains("atlantafx"), 
                "User Agent Stylesheet should be from AtlantaFX: " + userAgentStylesheet);
            
            // Verify current theme
            AtlantaFXTheme currentTheme = ThemeManager.getInstance().getCurrentTheme();
            assertNotNull(currentTheme, "Current theme should be set");
            System.out.println("Current theme: " + currentTheme.getDisplayName());
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("AtlantaFX Integration Test");
            primaryStage.show();
            
            // Close after verification
            Platform.runLater(() -> {
                primaryStage.close();
                Platform.exit();
            });
        }
    }
}
