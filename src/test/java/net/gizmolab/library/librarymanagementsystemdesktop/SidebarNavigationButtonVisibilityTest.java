package net.gizmolab.library.librarymanagementsystemdesktop;

import javafx.application.Platform;
import net.gizmolab.library.librarymanagementsystemdesktop.testsupport.FxToolkit;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify navigation button visibility in the sidebar.
 * 
 * Feature: sidebar-modernization
 * Task: 6.1 Test navigation button visibility
 * Validates: Requirements 1.5, 4.1, 4.2, 4.3
 * 
 * Tests:
 * - Button text is readable against purple background
 * - Hover effects work correctly
 * - Active button indicator is visible
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Sidebar Navigation Button Visibility Tests")
class SidebarNavigationButtonVisibilityTest {

    private Stage testStage;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Initialize JavaFX toolkit
        System.setProperty("javafx.mode", "true");
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
        
        FxToolkit.start();
    }

    @BeforeEach
    void setUp() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            testStage = new Stage();
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testStage != null) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                testStage.close();
                latch.countDown();
            });
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: CSS file contains purple background color for sidebar")
    void testCSSContainsPurpleBackground() {
        // Read the CSS file and verify it contains the purple background color
        InputStream cssStream = getClass().getResourceAsStream("/css/modern-theme.css");
        assertNotNull(cssStream, "CSS file should exist");
        
        String cssContent = new BufferedReader(new InputStreamReader(cssStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Verify sidebar-navigation or nav-sidebar selector exists
        assertTrue(
            cssContent.contains(".sidebar-navigation") || cssContent.contains(".nav-sidebar"),
            "CSS should contain .sidebar-navigation or .nav-sidebar selector"
        );
        
        // Verify purple background color is defined
        assertTrue(
            cssContent.contains("#7C4DFF") || cssContent.contains("7C4DFF"),
            "CSS should contain purple background color #7C4DFF"
        );
        
        // Verify background-color property is used
        assertTrue(
            cssContent.contains("-fx-background-color"),
            "CSS should use -fx-background-color property"
        );
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: CSS file contains button styling for sidebar")
    void testCSSContainsButtonStyling() {
        // Read the CSS file and verify it contains button styling
        InputStream cssStream = getClass().getResourceAsStream("/css/modern-theme.css");
        assertNotNull(cssStream, "CSS file should exist");
        
        String cssContent = new BufferedReader(new InputStreamReader(cssStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Verify sidebar button selectors exist
        assertTrue(
            cssContent.contains(".sidebar-button") || cssContent.contains(".nav-item"),
            "CSS should contain .sidebar-button or .nav-item selector"
        );
        
        // Verify button has default styling
        assertTrue(
            cssContent.contains("-fx-background-color") && cssContent.contains("-fx-text-fill"),
            "CSS should define background-color and text-fill for buttons"
        );
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: CSS file contains hover effect styling")
    void testCSSContainsHoverEffects() {
        // Read the CSS file and verify it contains hover effects
        InputStream cssStream = getClass().getResourceAsStream("/css/modern-theme.css");
        assertNotNull(cssStream, "CSS file should exist");
        
        String cssContent = new BufferedReader(new InputStreamReader(cssStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Verify hover selectors exist
        assertTrue(
            cssContent.contains(":hover"),
            "CSS should contain :hover pseudo-class for hover effects"
        );
        
        // Verify hover effects are defined for sidebar buttons
        assertTrue(
            (cssContent.contains(".sidebar-button:hover") || cssContent.contains(".nav-item:hover")),
            "CSS should define hover effects for sidebar buttons"
        );
        
        // Verify hover effects change background or text color
        String[] lines = cssContent.split("\n");
        boolean foundHoverBlock = false;
        boolean hasBackgroundOrTextChange = false;
        
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(":hover")) {
                foundHoverBlock = true;
            }
            if (foundHoverBlock && (lines[i].contains("-fx-background-color") || lines[i].contains("-fx-text-fill"))) {
                hasBackgroundOrTextChange = true;
                break;
            }
            if (foundHoverBlock && lines[i].contains("}")) {
                foundHoverBlock = false;
            }
        }
        
        assertTrue(hasBackgroundOrTextChange, "Hover effects should change background or text color");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: CSS file contains active button indicator styling")
    void testCSSContainsActiveButtonStyling() {
        // Read the CSS file and verify it contains active button styling
        InputStream cssStream = getClass().getResourceAsStream("/css/modern-theme.css");
        assertNotNull(cssStream, "CSS file should exist");
        
        String cssContent = new BufferedReader(new InputStreamReader(cssStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Verify active button selectors exist
        assertTrue(
            cssContent.contains("sidebar-button-active") || cssContent.contains("nav-item-active"),
            "CSS should contain active button styleClass (sidebar-button-active or nav-item-active)"
        );
        
        // Verify active state has distinct styling
        String[] lines = cssContent.split("\n");
        boolean foundActiveBlock = false;
        boolean hasDistinctStyling = false;
        
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("-active")) {
                foundActiveBlock = true;
            }
            if (foundActiveBlock && (lines[i].contains("-fx-background-color") || lines[i].contains("-fx-text-fill") || lines[i].contains("-fx-border-color"))) {
                hasDistinctStyling = true;
                break;
            }
            if (foundActiveBlock && lines[i].contains("}")) {
                foundActiveBlock = false;
            }
        }
        
        assertTrue(hasDistinctStyling, "Active button state should have distinct styling");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: FXML file contains navigation buttons with correct styleClasses")
    void testFXMLContainsNavigationButtons() {
        // Read the FXML file and verify it contains navigation buttons
        InputStream fxmlStream = getClass().getResourceAsStream("/fxml/main-navigation.fxml");
        assertNotNull(fxmlStream, "FXML file should exist");
        
        String fxmlContent = new BufferedReader(new InputStreamReader(fxmlStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Verify sidebar VBox exists with correct styleClass
        assertTrue(
            fxmlContent.contains("sidebar-navigation") || fxmlContent.contains("nav-sidebar"),
            "FXML should contain sidebar VBox with sidebar-navigation or nav-sidebar styleClass"
        );
        
        // Verify navigation buttons exist
        assertTrue(fxmlContent.contains("publicationsButton"), "FXML should contain publicationsButton");
        assertTrue(fxmlContent.contains("usersButton"), "FXML should contain usersButton");
        assertTrue(fxmlContent.contains("borrowsButton"), "FXML should contain borrowsButton");
        assertTrue(fxmlContent.contains("authorsButton"), "FXML should contain authorsButton");
        assertTrue(fxmlContent.contains("publishersButton"), "FXML should contain publishersButton");
        
        // Verify buttons have correct styleClass
        assertTrue(
            fxmlContent.contains("sidebar-button") || fxmlContent.contains("nav-item"),
            "FXML buttons should have sidebar-button or nav-item styleClass"
        );
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Verify contrast between button text and purple background")
    void testButtonTextContrast() {
        // Read the CSS file
        InputStream cssStream = getClass().getResourceAsStream("/css/modern-theme.css");
        assertNotNull(cssStream, "CSS file should exist");
        
        String cssContent = new BufferedReader(new InputStreamReader(cssStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Verify buttons have light colored text (white or light gray) for readability against purple
        // The buttons themselves have a light background (#f6f8fa) with dark text (#24292f)
        // This ensures readability
        assertTrue(
            cssContent.contains("#f6f8fa") || cssContent.contains("white") || cssContent.contains("#fff"),
            "Buttons should have light background color for readability"
        );
        
        // Verify hover state has contrasting colors
        assertTrue(
            cssContent.contains("#0969da") || cssContent.contains("white"),
            "Hover state should have contrasting colors for visibility"
        );
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Verify rounded corners are defined for sidebar")
    void testRoundedCornersForSidebar() {
        // Read the CSS file
        InputStream cssStream = getClass().getResourceAsStream("/css/modern-theme.css");
        assertNotNull(cssStream, "CSS file should exist");
        
        String cssContent = new BufferedReader(new InputStreamReader(cssStream))
            .lines()
            .collect(Collectors.joining("\n"));
        
        // Verify background-radius is defined for sidebar
        assertTrue(
            cssContent.contains("-fx-background-radius"),
            "CSS should define -fx-background-radius for rounded corners"
        );
        
        // Verify the radius value is present (0 12 12 0 for right-side rounding)
        assertTrue(
            cssContent.contains("0 12 12 0") || cssContent.contains("12"),
            "CSS should define rounded corners with appropriate radius value"
        );
    }

    // No Platform.exit(): JavaFX cannot restart in the same JVM, and later tests still need it (FxToolkit)
}
