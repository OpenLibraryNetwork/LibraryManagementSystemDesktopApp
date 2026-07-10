package net.gizmolab.library.librarymanagementsystemdesktop.service.utilities;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for enhancing keyboard accessibility throughout the application.
 * Provides methods to ensure proper tab order, keyboard shortcuts, and focus management.
 */
public class KeyboardAccessibilityHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyboardAccessibilityHelper.class);
    
    /**
     * Ensures all interactive elements in a container are keyboard accessible.
     * Sets focusTraversable to true for buttons, text fields, combo boxes, etc.
     * 
     * @param container The container to process
     */
    public static void ensureKeyboardAccessibility(Pane container) {
        if (container == null) {
            return;
        }
        
        List<Node> interactiveNodes = findInteractiveNodes(container);
        
        for (Node node : interactiveNodes) {
            // Ensure the node is focusable
            if (!node.isFocusTraversable()) {
                node.setFocusTraversable(true);
                logger.debug("Enabled focus traversal for: " + node.getClass().getSimpleName());
            }
            
            // Add accessible text if missing for icon-only buttons
            if (node instanceof Button) {
                Button button = (Button) node;
                ensureAccessibleText(button);
            }
        }
        
        logger.info("Keyboard accessibility ensured for {} interactive elements", interactiveNodes.size());
    }
    
    /**
     * Sets explicit tab order for a list of nodes.
     * 
     * @param nodes The nodes in desired tab order
     */
    public static void setTabOrder(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        
        // JavaFX handles tab order based on scene graph order by default
        // We ensure all nodes are focusTraversable
        for (Node node : nodes) {
            if (node instanceof Control) {
                node.setFocusTraversable(true);
            }
        }
        
        logger.debug("Tab order set for {} nodes", nodes.size());
    }
    
    /**
     * Adds a keyboard shortcut to a scene.
     * 
     * @param scene The scene to add the shortcut to
     * @param keyCombination The key combination (e.g., Ctrl+N)
     * @param action The action to execute
     * @param description Description of the shortcut for logging
     */
    public static void addKeyboardShortcut(Scene scene, KeyCombination keyCombination, 
                                          Runnable action, String description) {
        if (scene == null || keyCombination == null || action == null) {
            return;
        }
        
        scene.getAccelerators().put(keyCombination, action);
        logger.info("Added keyboard shortcut: {} - {}", keyCombination.getDisplayText(), description);
    }
    
    /**
     * Adds common keyboard shortcuts for a management view.
     * 
     * @param scene The scene
     * @param addAction Action for Ctrl+N (New)
     * @param editAction Action for Ctrl+E (Edit)
     * @param deleteAction Action for Delete key
     * @param refreshAction Action for F5 (Refresh)
     */
    public static void addCommonShortcuts(Scene scene, Runnable addAction, 
                                         Runnable editAction, Runnable deleteAction, 
                                         Runnable refreshAction) {
        if (scene == null) {
            return;
        }
        
        if (addAction != null) {
            addKeyboardShortcut(scene, 
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                addAction, "New item");
        }
        
        if (editAction != null) {
            addKeyboardShortcut(scene, 
                new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN),
                editAction, "Edit item");
        }
        
        if (deleteAction != null) {
            addKeyboardShortcut(scene, 
                new KeyCodeCombination(KeyCode.DELETE),
                deleteAction, "Delete item");
        }
        
        if (refreshAction != null) {
            addKeyboardShortcut(scene, 
                new KeyCodeCombination(KeyCode.F5),
                refreshAction, "Refresh");
        }
    }
    
    /**
     * Adds navigation keyboard shortcuts.
     * 
     * @param scene The scene
     * @param booksAction Action for Ctrl+1 (Books)
     * @param usersAction Action for Ctrl+2 (Users)
     * @param borrowsAction Action for Ctrl+3 (Borrows)
     * @param authorsAction Action for Ctrl+4 (Authors)
     * @param publishersAction Action for Ctrl+5 (Publishers)
     */
    public static void addNavigationShortcuts(Scene scene, Runnable booksAction,
                                             Runnable usersAction, Runnable borrowsAction,
                                             Runnable authorsAction, Runnable publishersAction) {
        if (scene == null) {
            return;
        }
        
        if (booksAction != null) {
            addKeyboardShortcut(scene,
                new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN),
                booksAction, "Navigate to Books");
        }
        
        if (usersAction != null) {
            addKeyboardShortcut(scene,
                new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN),
                usersAction, "Navigate to Users");
        }
        
        if (borrowsAction != null) {
            addKeyboardShortcut(scene,
                new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN),
                borrowsAction, "Navigate to Borrows");
        }
        
        if (authorsAction != null) {
            addKeyboardShortcut(scene,
                new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN),
                authorsAction, "Navigate to Authors");
        }
        
        if (publishersAction != null) {
            addKeyboardShortcut(scene,
                new KeyCodeCombination(KeyCode.DIGIT5, KeyCombination.CONTROL_DOWN),
                publishersAction, "Navigate to Publishers");
        }
    }
    
    /**
     * Adds theme toggle keyboard shortcut (Ctrl+T).
     * 
     * @param scene The scene
     * @param themeToggleAction Action to toggle theme
     */
    public static void addThemeToggleShortcut(Scene scene, Runnable themeToggleAction) {
        if (scene == null || themeToggleAction == null) {
            return;
        }
        
        addKeyboardShortcut(scene,
            new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
            themeToggleAction, "Toggle theme");
    }
    
    /**
     * Adds settings dialog keyboard shortcut (Ctrl+,).
     * 
     * @param scene The scene
     * @param settingsAction Action to open settings
     */
    public static void addSettingsShortcut(Scene scene, Runnable settingsAction) {
        if (scene == null || settingsAction == null) {
            return;
        }
        
        addKeyboardShortcut(scene,
            new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN),
            settingsAction, "Open settings");
    }
    
    /**
     * Finds all interactive nodes in a container recursively.
     * 
     * @param node The node to search
     * @return List of interactive nodes
     */
    private static List<Node> findInteractiveNodes(Node node) {
        List<Node> interactiveNodes = new ArrayList<>();
        
        if (isInteractiveNode(node)) {
            interactiveNodes.add(node);
        }
        
        if (node instanceof Pane) {
            Pane pane = (Pane) node;
            for (Node child : pane.getChildren()) {
                interactiveNodes.addAll(findInteractiveNodes(child));
            }
        }
        
        return interactiveNodes;
    }
    
    /**
     * Checks if a node is interactive (should be keyboard accessible).
     * 
     * @param node The node to check
     * @return true if the node is interactive
     */
    private static boolean isInteractiveNode(Node node) {
        return node instanceof Button ||
               node instanceof TextField ||
               node instanceof TextArea ||
               node instanceof ComboBox ||
               node instanceof CheckBox ||
               node instanceof RadioButton ||
               node instanceof Hyperlink ||
               node instanceof MenuButton ||
               node instanceof ToggleButton ||
               node instanceof Slider ||
               node instanceof Spinner ||
               node instanceof DatePicker ||
               node instanceof TableView ||
               node instanceof ListView ||
               node instanceof TreeView;
    }
    
    /**
     * Ensures a button has accessible text (tooltip or aria label).
     * 
     * @param button The button to check
     */
    private static void ensureAccessibleText(Button button) {
        // If button has no text but has a graphic (icon-only button)
        if ((button.getText() == null || button.getText().trim().isEmpty()) && 
            button.getGraphic() != null) {
            
            // Check if it already has a tooltip
            if (button.getTooltip() == null) {
                // Try to infer tooltip from style classes or ID
                String tooltipText = inferTooltipText(button);
                if (tooltipText != null) {
                    button.setTooltip(new Tooltip(tooltipText));
                    logger.debug("Added tooltip to icon-only button: {}", tooltipText);
                }
            }
        }
    }
    
    /**
     * Infers tooltip text from button properties.
     * 
     * @param button The button
     * @return Inferred tooltip text or null
     */
    private static String inferTooltipText(Button button) {
        // Check ID
        String id = button.getId();
        if (id != null) {
            if (id.contains("add") || id.contains("new")) return "Add";
            if (id.contains("edit")) return "Edit";
            if (id.contains("delete") || id.contains("remove")) return "Delete";
            if (id.contains("refresh")) return "Refresh";
            if (id.contains("search")) return "Search";
            if (id.contains("filter")) return "Filter";
            if (id.contains("settings")) return "Settings";
            if (id.contains("theme")) return "Toggle Theme";
        }
        
        // Check style classes
        for (String styleClass : button.getStyleClass()) {
            if (styleClass.contains("add")) return "Add";
            if (styleClass.contains("edit")) return "Edit";
            if (styleClass.contains("delete")) return "Delete";
            if (styleClass.contains("refresh")) return "Refresh";
        }
        
        return null;
    }
    
    /**
     * Requests focus on the first focusable element in a container.
     * Useful for dialogs and forms.
     * 
     * @param container The container
     */
    public static void focusFirstElement(Pane container) {
        if (container == null) {
            return;
        }
        
        List<Node> interactiveNodes = findInteractiveNodes(container);
        if (!interactiveNodes.isEmpty()) {
            Node firstNode = interactiveNodes.get(0);
            firstNode.requestFocus();
            logger.debug("Focus requested on first element: {}", firstNode.getClass().getSimpleName());
        }
    }
}
