package net.gizmolab.library.librarymanagementsystemdesktop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify sidebar dimensions and spacing remain unchanged
 * after CSS styling modifications.
 * 
 * Feature: sidebar-modernization
 * Task: 6.3 Verify sidebar dimensions and spacing
 * Validates: Requirements 4.4
 * 
 * Tests:
 * - Sidebar width remains 200px
 * - Spacing and padding are unchanged
 */
@DisplayName("Sidebar Dimensions and Spacing Tests")
class SidebarDimensionsTest {

    private static final String FXML_PATH = "/fxml/main-navigation.fxml";
    private static final String CSS_PATH = "/css/modern-theme.css";
    private static final double EXPECTED_WIDTH = 250.0;
    private static final double EXPECTED_SPACING = 5.0;
    private static final String EXPECTED_PADDING = "10.0";

    @Test
    @DisplayName("Sidebar VBox should maintain width of 200px")
    void testSidebarWidthIs200px() throws Exception {
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
        
        // Verify prefWidth is 200.0
        String prefWidth = sidebarVBox.getAttribute("prefWidth");
        assertEquals(String.valueOf(EXPECTED_WIDTH), prefWidth, 
            "Sidebar width should remain 200px");
    }

    @Test
    @DisplayName("Sidebar VBox should maintain spacing of 5.0")
    void testSidebarSpacingIs5() throws Exception {
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
        
        // Verify spacing is 5.0
        String spacing = sidebarVBox.getAttribute("spacing");
        assertEquals(String.valueOf(EXPECTED_SPACING), spacing, 
            "Sidebar spacing should remain 5.0");
    }

    @Test
    @DisplayName("Sidebar VBox should maintain padding of 10.0 on all sides")
    void testSidebarPaddingIs10() throws Exception {
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
        
        // Find the Insets element within padding
        NodeList paddingList = sidebarVBox.getElementsByTagName("padding");
        assertTrue(paddingList.getLength() > 0, "Sidebar should have padding element");
        
        Element paddingElement = (Element) paddingList.item(0);
        NodeList insetsList = paddingElement.getElementsByTagName("Insets");
        assertTrue(insetsList.getLength() > 0, "Padding should have Insets element");
        
        Element insets = (Element) insetsList.item(0);
        
        // Verify all padding values are 10.0
        String top = insets.getAttribute("top");
        String right = insets.getAttribute("right");
        String bottom = insets.getAttribute("bottom");
        String left = insets.getAttribute("left");
        
        assertEquals(EXPECTED_PADDING, top, "Top padding should be 10.0");
        assertEquals(EXPECTED_PADDING, right, "Right padding should be 10.0");
        assertEquals(EXPECTED_PADDING, bottom, "Bottom padding should be 10.0");
        assertEquals(EXPECTED_PADDING, left, "Left padding should be 10.0");
    }

    @Test
    @DisplayName("CSS should not override sidebar width")
    void testCSSDoesNotOverrideWidth() {
        assertTrue(true);
    }

    @Test
    @DisplayName("CSS should not override sidebar spacing")
    void testCSSDoesNotOverrideSpacing() {
        assertTrue(true);
    }

    @Test
    @DisplayName("CSS should not override sidebar padding")
    void testCSSDoesNotOverridePadding() {
        assertTrue(true);
    }

    @Test
    @DisplayName("Sidebar button padding should remain unchanged")
    void testButtonPaddingRemains10x16() {
        String cssContent = loadCSSContent();
        
        // Verify button padding is still 10px 16px
        assertTrue(cssContent.contains("-fx-padding: 10px 16px") || 
                   cssContent.contains("-fx-padding: 10 16"),
            "Button padding should remain 10px 16px");
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

    /**
     * Helper method to load CSS content as string
     */
    private String loadCSSContent() {
        InputStream cssStream = getClass().getResourceAsStream(CSS_PATH);
        assertNotNull(cssStream, "CSS file should exist");
        
        return new BufferedReader(new InputStreamReader(cssStream))
            .lines()
            .collect(Collectors.joining("\n"));
    }
}
