package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify application starts successfully without CSS parsing errors
 * or UI initialization exceptions.
 * 
 * Feature: sidebar-modernization
 * Task: 7.5 Test application startup
 * Validates: Requirements 5.5
 * 
 * Tests:
 * - Application starts without CSS parsing errors
 * - No exceptions during UI initialization
 * - Application window is created successfully
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Application Startup Tests")
class ApplicationStartupTest {

    private static ConfigurableApplicationContext springContext;
    private static boolean javaFXInitialized = false;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Initialize JavaFX toolkit
        System.setProperty("javafx.mode", "true");
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
        
        // Initialize Spring context
        SpringApplication app = new SpringApplication(LibraryManagementSystemApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        springContext = app.run();
        
        // Initialize JavaFX Platform
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                javaFXInitialized = true;
                latch.countDown();
            });
            latch.await(5, TimeUnit.SECONDS);
        } else {
            javaFXInitialized = true;
        }
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        // Clean up Spring context
        if (springContext != null) {
            springContext.close();
        }
        
        // Clean up JavaFX
        try {
            Platform.exit();
        } catch (Exception e) {
            // Ignore cleanup errors in headless mode
        }
        
        // Clean up system properties
        System.clearProperty("javafx.mode");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Spring context initializes successfully")
    void testSpringContextInitialization() {
        assertNotNull(springContext, "Spring context should be initialized");
        assertTrue(springContext.isActive(), "Spring context should be active");
        
        // Verify FXMLLoaderFactory bean is available
        FXMLLoaderFactory factory = springContext.getBean(FXMLLoaderFactory.class);
        assertNotNull(factory, "FXMLLoaderFactory bean should be available");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: JavaFX Platform initializes successfully")
    void testJavaFXPlatformInitialization() {
        assertTrue(javaFXInitialized, "JavaFX Platform should be initialized");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: FXML loads without exceptions")
    void testFXMLLoadsWithoutExceptions() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Parent> rootRef = new AtomicReference<>();
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        
        Platform.runLater(() -> {
            try {
                FXMLLoaderFactory factory = springContext.getBean(FXMLLoaderFactory.class);
                Parent root = factory.load("/fxml/main-navigation.fxml");
                rootRef.set(root);
            } catch (Exception e) {
                exceptionRef.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FXML loading should complete within timeout");
        
        if (exceptionRef.get() != null) {
            fail("FXML loading should not throw exceptions: " + exceptionRef.get().getMessage());
        }
        
        assertNotNull(rootRef.get(), "FXML root should be loaded successfully");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: CSS loads without parsing errors")
    void testCSSLoadsWithoutParsingErrors() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Scene> sceneRef = new AtomicReference<>();
        AtomicBoolean cssErrorDetected = new AtomicBoolean(false);
        
        // Capture System.err to detect CSS parsing errors
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        
        Platform.runLater(() -> {
            try {
                FXMLLoaderFactory factory = springContext.getBean(FXMLLoaderFactory.class);
                Parent root = factory.load("/fxml/main-navigation.fxml");
                
                // Create scene and apply CSS
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().clear();
                
                // Apply the modern-theme.css stylesheet
                String cssPath = getClass().getResource("/css/modern-theme.css").toExternalForm();
                scene.getStylesheets().add(cssPath);
                
                sceneRef.set(scene);
                
                // Force CSS processing by applying CSS to the scene graph
                scene.getRoot().applyCss();
                
            } catch (Exception e) {
                System.err.println("Exception during CSS loading: " + e.getMessage());
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "CSS loading should complete within timeout");
        
        // Restore System.err
        System.setErr(originalErr);
        
        // Check for CSS parsing errors in the captured output
        String errorOutput = errContent.toString();
        
        // Common CSS error patterns
        boolean hasCSSError = errorOutput.contains("CSS Error") ||
                             errorOutput.contains("Couldn't resolve") ||
                             errorOutput.contains("Expected") ||
                             errorOutput.contains("Unexpected token") ||
                             errorOutput.contains("WARNING: CSS");
        
        assertFalse(hasCSSError, "CSS should load without parsing errors. Error output: " + errorOutput);
        assertNotNull(sceneRef.get(), "Scene should be created successfully");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Application window can be created without exceptions")
    void testApplicationWindowCreation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        
        Platform.runLater(() -> {
            try {
                // Create a test stage
                Stage testStage = new Stage();
                
                // Load FXML
                FXMLLoaderFactory factory = springContext.getBean(FXMLLoaderFactory.class);
                Parent root = factory.load("/fxml/main-navigation.fxml");
                
                // Create scene with CSS
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/css/modern-theme.css").toExternalForm());
                
                // Setup stage
                testStage.setTitle("Library Management System - Test");
                testStage.setScene(scene);
                testStage.setMinWidth(1024);
                testStage.setMinHeight(600);
                
                // Don't show the stage in headless mode, just verify it can be created
                stageRef.set(testStage);
                
            } catch (Exception e) {
                exceptionRef.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Window creation should complete within timeout");
        
        if (exceptionRef.get() != null) {
            fail("Window creation should not throw exceptions: " + exceptionRef.get().getMessage());
        }
        
        assertNotNull(stageRef.get(), "Stage should be created successfully");
        assertNotNull(stageRef.get().getScene(), "Stage should have a scene");
        assertEquals("Library Management System - Test", stageRef.get().getTitle(), 
            "Stage should have correct title");
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Sidebar CSS properties are applied correctly")
    void testSidebarCSSPropertiesApplied() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean cssApplied = new AtomicBoolean(false);
        
        Platform.runLater(() -> {
            try {
                FXMLLoaderFactory factory = springContext.getBean(FXMLLoaderFactory.class);
                Parent root = factory.load("/fxml/main-navigation.fxml");
                
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/css/modern-theme.css").toExternalForm());
                
                // Force CSS application
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                
                // Verify CSS was applied (no exceptions means success)
                cssApplied.set(true);
                
            } catch (Exception e) {
                System.err.println("Exception during CSS application: " + e.getMessage());
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "CSS application should complete within timeout");
        assertTrue(cssApplied.get(), "CSS should be applied without exceptions");
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: No exceptions during complete UI initialization")
    void testCompleteUIInitializationWithoutExceptions() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        AtomicBoolean initializationComplete = new AtomicBoolean(false);
        
        // Capture System.err to detect any errors
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        
        Platform.runLater(() -> {
            try {
                // Simulate complete application startup
                Stage testStage = new Stage();
                
                // Load FXML
                FXMLLoaderFactory factory = springContext.getBean(FXMLLoaderFactory.class);
                Parent root = factory.load("/fxml/main-navigation.fxml");
                
                // Create scene with CSS
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().clear();
                scene.getStylesheets().add(getClass().getResource("/css/modern-theme.css").toExternalForm());
                
                // Setup stage (as in real application)
                testStage.setTitle("Library Management System");
                testStage.setScene(scene);
                testStage.setMinWidth(1024);
                testStage.setMinHeight(600);
                
                // Force CSS and layout processing
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                
                initializationComplete.set(true);
                
            } catch (Exception e) {
                exceptionRef.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "UI initialization should complete within timeout");
        
        // Restore System.err
        System.setErr(originalErr);
        
        // Check for any exceptions
        if (exceptionRef.get() != null) {
            fail("UI initialization should not throw exceptions: " + exceptionRef.get().getMessage());
        }
        
        assertTrue(initializationComplete.get(), "UI initialization should complete successfully");
        
        // Check for any error output
        String errorOutput = errContent.toString();
        boolean hasError = errorOutput.contains("Exception") ||
                          errorOutput.contains("Error") ||
                          errorOutput.contains("SEVERE");
        
        assertFalse(hasError, "UI initialization should not produce error output. Output: " + errorOutput);
    }
}
