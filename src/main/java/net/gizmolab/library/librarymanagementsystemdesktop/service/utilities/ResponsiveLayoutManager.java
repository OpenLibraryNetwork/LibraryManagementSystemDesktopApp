package net.gizmolab.library.librarymanagementsystemdesktop.service.utilities;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing responsive layout behavior.
 * Handles window resize events and applies appropriate style classes
 * for responsive layout adjustments.
 */
public class ResponsiveLayoutManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponsiveLayoutManager.class);
    
    // Breakpoints
    private static final double COMPACT_MODE_WIDTH = 1280.0;
    private static final double MINIMUM_WIDTH = 1024.0;
    
    // Debounce duration for resize events (200ms as per requirements)
    private static final Duration RESIZE_DEBOUNCE = Duration.millis(200);
    
    private final Stage stage;
    private final Scene scene;
    private final BooleanProperty compactMode;
    private PauseTransition resizeDebouncer;
    
    /**
     * Creates a new ResponsiveLayoutManager for the given stage.
     *
     * @param stage The primary stage to manage
     */
    public ResponsiveLayoutManager(Stage stage) {
        this.stage = stage;
        this.scene = stage.getScene();
        this.compactMode = new SimpleBooleanProperty(false);
        
        initializeResizeHandling();
        
        logger.info("ResponsiveLayoutManager initialized for stage");
    }
    
    /**
     * Initializes window resize event handling with debouncing.
     */
    private void initializeResizeHandling() {
        // Create debouncer for resize events
        resizeDebouncer = new PauseTransition(RESIZE_DEBOUNCE);
        resizeDebouncer.setOnFinished(event -> handleResize());
        
        // Listen to width changes
        stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            // Restart the debouncer on each resize event
            resizeDebouncer.playFromStart();
        });
        
        // Listen to height changes
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            // Restart the debouncer on each resize event
            resizeDebouncer.playFromStart();
        });
        
        // Initial layout adjustment
        handleResize();
    }
    
    /**
     * Handles window resize events and applies appropriate responsive styles.
     * This method is debounced to ensure it completes within 200ms of the last resize event.
     */
    private void handleResize() {
        double width = stage.getWidth();
        double height = stage.getHeight();
        
        logger.debug("Window resized to {}x{}", width, height);
        
        // Determine if compact mode should be enabled
        boolean shouldBeCompact = width < COMPACT_MODE_WIDTH;
        
        if (shouldBeCompact != compactMode.get()) {
            compactMode.set(shouldBeCompact);
            applyResponsiveStyles(shouldBeCompact);
            logger.info("Compact mode {}", shouldBeCompact ? "enabled" : "disabled");
        }
        
        // Warn if window is below minimum width
        if (width < MINIMUM_WIDTH) {
            logger.warn("Window width ({}) is below minimum recommended width ({})", width, MINIMUM_WIDTH);
        }
    }
    
    /**
     * Applies or removes responsive style classes based on compact mode.
     *
     * @param compact Whether compact mode should be enabled
     */
    private void applyResponsiveStyles(boolean compact) {
        if (scene == null || scene.getRoot() == null) {
            logger.warn("Scene or root is null, cannot apply responsive styles");
            return;
        }
        
        Node root = scene.getRoot();
        
        if (compact) {
            if (!root.getStyleClass().contains("compact-mode")) {
                root.getStyleClass().add("compact-mode");
            }
        } else {
            root.getStyleClass().remove("compact-mode");
        }
    }
    
    /**
     * Applies responsive style classes to a specific node.
     * This is useful for individual components that need responsive behavior.
     *
     * @param node The node to apply responsive styles to
     * @param styleClass The base style class (e.g., "responsive-container")
     */
    public static void applyResponsiveStyles(Node node, String styleClass) {
        if (node == null || styleClass == null) {
            return;
        }
        
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }
    
}
