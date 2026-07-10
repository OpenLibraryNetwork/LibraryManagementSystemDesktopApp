package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LibraryManagementFXApplication.
 * Tests the Spring Boot integration without actually starting JavaFX.
 */
class LibraryManagementFXApplicationTest {

    @Test
    void testSpringContextInitialization() {
        // Set JavaFX mode property
        System.setProperty("javafx.mode", "true");
        
        try {
            // Initialize Spring Boot context as the JavaFX application would
            SpringApplication app = new SpringApplication(LibraryManagementSystemApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            
            ConfigurableApplicationContext context = app.run();
            
            // Verify that the context is initialized
            assertNotNull(context);
            assertTrue(context.isActive());
            
            // Verify that FXMLLoaderFactory bean is available
            FXMLLoaderFactory fxmlLoaderFactory = context.getBean(FXMLLoaderFactory.class);
            assertNotNull(fxmlLoaderFactory);
            
            // Clean up
            context.close();
            
        } finally {
            // Clean up system property
            System.clearProperty("javafx.mode");
        }
    }

    @Test
    void testFXMLLoaderFactoryCreation() {
        // Set JavaFX mode property
        System.setProperty("javafx.mode", "true");
        
        try {
            SpringApplication app = new SpringApplication(LibraryManagementSystemApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            
            ConfigurableApplicationContext context = app.run();
            
            FXMLLoaderFactory factory = context.getBean(FXMLLoaderFactory.class);
            
            // Test that we can create a loader (even though the FXML doesn't exist yet)
            assertThrows(IllegalArgumentException.class, () -> {
                factory.createLoader("/fxml/non-existent.fxml");
            });
            
            context.close();
            
        } finally {
            System.clearProperty("javafx.mode");
        }
    }
}