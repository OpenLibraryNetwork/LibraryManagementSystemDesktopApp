package net.gizmolab.library.librarymanagementsystemdesktop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that the FXML file structure remains unchanged
 * and that CSS-only changes were made without touching FXML.
 * 
 * Feature: sidebar-modernization
 * Task: 5.1 Test that FXML file remains unchanged
 * Validates: Requirements 3.5
 */
@DisplayName("Sidebar FXML Structure Tests")
class SidebarFXMLStructureTest {

    private static final String FXML_PATH = "/fxml/main-navigation.fxml";

    @Test
    @DisplayName("FXML file should exist and be loadable")
    void testFXMLFileExists() {
        InputStream fxmlStream = getClass().getResourceAsStream(FXML_PATH);
        assertNotNull(fxmlStream, "FXML file should exist at " + FXML_PATH);
    }

    @Test
    @DisplayName("VBox container should have correct styleClass attributes")
    void testVBoxHasCorrectStyleClass() throws Exception {
        Document doc = loadFXMLDocument();
        
        // Find the VBox element in the left section (sidebar)
        NodeList vboxList = doc.getElementsByTagName("VBox");
        
        boolean foundSidebarVBox = false;
        for (int i = 0; i < vboxList.getLength(); i++) {
            Element vbox = (Element) vboxList.item(i);
            String styleClass = vbox.getAttribute("styleClass");
            
            // Check if this VBox has the sidebar styleClass
            if (styleClass.contains("sidebar-navigation") || styleClass.contains("nav-sidebar")) {
                foundSidebarVBox = true;
                
                // Verify it has the correct styleClass
                assertTrue(styleClass.contains("sidebar-navigation") || styleClass.contains("nav-sidebar"),
                    "VBox should have 'sidebar-navigation' or 'nav-sidebar' styleClass");
                
                // Verify it has the correct width
                String prefWidth = vbox.getAttribute("prefWidth");
                assertEquals("250.0", prefWidth, "Sidebar VBox should have prefWidth of 250.0");
                
                break;
            }
        }
        
        assertTrue(foundSidebarVBox, "Should find a VBox with sidebar-navigation or nav-sidebar styleClass");
    }

    @Test
    @DisplayName("Sidebar VBox should contain navigation buttons")
    void testSidebarContainsNavigationButtons() throws Exception {
        Document doc = loadFXMLDocument();
        
        // Find the sidebar VBox
        NodeList vboxList = doc.getElementsByTagName("VBox");
        Element sidebarVBox = null;
        
        for (int i = 0; i < vboxList.getLength(); i++) {
            Element vbox = (Element) vboxList.item(i);
            String styleClass = vbox.getAttribute("styleClass");
            if (styleClass.contains("sidebar-navigation") || styleClass.contains("nav-sidebar")) {
                sidebarVBox = vbox;
                break;
            }
        }
        
        assertNotNull(sidebarVBox, "Should find sidebar VBox");
        
        // Get all Button elements within the sidebar VBox
        NodeList buttons = sidebarVBox.getElementsByTagName("Button");
        
        // Verify we have the expected navigation buttons
        assertTrue(buttons.getLength() >= 5, 
            "Sidebar should contain at least 5 navigation buttons (Books, Users, Borrows, Authors, Publishers)");
        
        // Verify button IDs exist
        boolean hasBooksButton = false;
        boolean hasUsersButton = false;
        boolean hasBorrowsButton = false;
        boolean hasAuthorsButton = false;
        boolean hasPublishersButton = false;
        
        for (int i = 0; i < buttons.getLength(); i++) {
            Element button = (Element) buttons.item(i);
            String fxId = button.getAttribute("fx:id");
            
            if ("booksButton".equals(fxId)) hasBooksButton = true;
            if ("usersButton".equals(fxId)) hasUsersButton = true;
            if ("borrowsButton".equals(fxId)) hasBorrowsButton = true;
            if ("authorsButton".equals(fxId)) hasAuthorsButton = true;
            if ("publishersButton".equals(fxId)) hasPublishersButton = true;
        }
        
        assertTrue(hasBooksButton, "Should have booksButton");
        assertTrue(hasUsersButton, "Should have usersButton");
        assertTrue(hasBorrowsButton, "Should have borrowsButton");
        assertTrue(hasAuthorsButton, "Should have authorsButton");
        assertTrue(hasPublishersButton, "Should have publishersButton");
    }

    @Test
    @DisplayName("FXML structure should not have inline background-color or background-radius styles")
    void testNoInlineBackgroundStyles() throws Exception {
        Document doc = loadFXMLDocument();
        
        // Find the sidebar VBox
        NodeList vboxList = doc.getElementsByTagName("VBox");
        
        for (int i = 0; i < vboxList.getLength(); i++) {
            Element vbox = (Element) vboxList.item(i);
            String styleClass = vbox.getAttribute("styleClass");
            
            if (styleClass.contains("sidebar-navigation") || styleClass.contains("nav-sidebar")) {
                // Check that there's no inline style attribute with background properties
                String style = vbox.getAttribute("style");
                
                if (style != null && !style.isEmpty()) {
                    assertFalse(style.contains("-fx-background-color"), 
                        "VBox should not have inline -fx-background-color style (should be in CSS)");
                    assertFalse(style.contains("-fx-background-radius"), 
                        "VBox should not have inline -fx-background-radius style (should be in CSS)");
                }
                
                break;
            }
        }
    }

    @Test
    @DisplayName("FXML should use BorderPane layout with sidebar in left section")
    void testBorderPaneLayout() throws Exception {
        Document doc = loadFXMLDocument();
        
        // Verify root element is BorderPane
        Element root = doc.getDocumentElement();
        assertEquals("BorderPane", root.getTagName(), "Root element should be BorderPane");
        
        // Find the left section
        NodeList leftSections = root.getElementsByTagName("left");
        assertTrue(leftSections.getLength() > 0, "BorderPane should have a left section");
        
        // Verify the left section contains the sidebar VBox
        Element leftSection = (Element) leftSections.item(0);
        NodeList vboxInLeft = leftSection.getElementsByTagName("VBox");
        assertTrue(vboxInLeft.getLength() > 0, "Left section should contain a VBox");
        
        Element sidebarVBox = (Element) vboxInLeft.item(0);
        String styleClass = sidebarVBox.getAttribute("styleClass");
        assertTrue(styleClass.contains("sidebar-navigation") || styleClass.contains("nav-sidebar"),
            "VBox in left section should have sidebar styleClass");
    }

    @Test
    @DisplayName("Sidebar buttons should have correct styleClass attributes")
    void testButtonStyleClasses() throws Exception {
        Document doc = loadFXMLDocument();
        
        // Find the sidebar VBox
        NodeList vboxList = doc.getElementsByTagName("VBox");
        Element sidebarVBox = null;
        
        for (int i = 0; i < vboxList.getLength(); i++) {
            Element vbox = (Element) vboxList.item(i);
            String styleClass = vbox.getAttribute("styleClass");
            if (styleClass.contains("sidebar-navigation") || styleClass.contains("nav-sidebar")) {
                sidebarVBox = vbox;
                break;
            }
        }
        
        assertNotNull(sidebarVBox, "Should find sidebar VBox");
        
        // Get all Button elements within the sidebar VBox
        NodeList buttons = sidebarVBox.getElementsByTagName("Button");
        
        // Verify each button has appropriate styleClass
        for (int i = 0; i < buttons.getLength(); i++) {
            Element button = (Element) buttons.item(i);
            String styleClass = button.getAttribute("styleClass");
            
            // Buttons should have sidebar-button or nav-item styleClass
            assertTrue(styleClass.contains("sidebar-button") || styleClass.contains("nav-item"),
                "Navigation buttons should have 'sidebar-button' or 'nav-item' styleClass");
        }
    }

    /**
     * Helper method to load and parse the FXML document
     */
    private Document loadFXMLDocument() throws Exception {
        InputStream fxmlStream = getClass().getResourceAsStream(FXML_PATH);
        assertNotNull(fxmlStream, "FXML file should exist");
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        return builder.parse(fxmlStream);
    }
}
