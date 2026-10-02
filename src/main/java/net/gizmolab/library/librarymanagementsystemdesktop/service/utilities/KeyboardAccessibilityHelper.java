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
public final class KeyboardAccessibilityHelper {

    private KeyboardAccessibilityHelper() {}
    
    private static final Logger logger = LoggerFactory.getLogger(KeyboardAccessibilityHelper.class);
    
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
        
        if (node instanceof Pane pane) {
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
    
}
