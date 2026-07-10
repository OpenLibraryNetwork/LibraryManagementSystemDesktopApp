package net.gizmolab.library.librarymanagementsystemdesktop.service;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Singleton utility for loading and providing themed icons using FontAwesome.
 * Implements LRU cache to prevent memory exhaustion.
 */
public class IconProvider {
    private static final Logger logger = LoggerFactory.getLogger(IconProvider.class);
    private static IconProvider instance;
    private static final int MAX_CACHE_SIZE = 100;
    
    // LRU cache implementation using LinkedHashMap
    private final Map<String, FontAwesomeIcon> iconCache;

    private IconProvider() {
        this.iconCache = new LinkedHashMap<String, FontAwesomeIcon>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, FontAwesomeIcon> eldest) {
                boolean shouldRemove = size() > MAX_CACHE_SIZE;
                if (shouldRemove) {
                    logger.debug("Removed eldest cache entry: {}", eldest.getKey());
                }
                return shouldRemove;
            }
        };
    }

    /**
     * Get the singleton instance of IconProvider.
     * @return IconProvider instance
     */
    public static synchronized IconProvider getInstance() {
        if (instance == null) {
            instance = new IconProvider();
        }
        return instance;
    }

    /**
     * Get an icon by name with specified size.
     * @param iconName Name of the icon (e.g., "icon-book", "icon-user")
     * @param size Size of the icon in pixels
     * @return Node representing the icon
     */
    public Node getIcon(String iconName, double size) {
        return getIcon(iconName, size, null);
    }

    /**
     * Get an icon by name with specified size and color.
     * @param iconName Name of the icon (e.g., "icon-book", "icon-user")
     * @param size Size of the icon in pixels
     * @param color Color for the icon (null for default)
     * @return Node representing the icon
     */
    public Node getIcon(String iconName, double size, String color) {
        FontAwesomeIcon faIcon = mapIconName(iconName);
        
        if (faIcon == null) {
            logger.warn("Icon not found: {}", iconName);
            faIcon = FontAwesomeIcon.CIRCLE;
        }
        
        FontAwesomeIconView iconView = new FontAwesomeIconView(faIcon);
        iconView.setSize(String.valueOf((int) size));
        
        if (color != null && !color.isEmpty()) {
            iconView.setStyle("-fx-fill: " + color + ";");
        }
        
        return iconView;
    }

    /**
     * Get a themed icon with specified color class.
     * @param iconName Name of the icon
     * @param size Size of the icon in pixels
     * @param colorClass CSS color class to apply
     * @return Node representing the themed icon
     */
    public Node getThemedIcon(String iconName, double size, String colorClass) {
        Node icon = getIcon(iconName, size);
        if (icon != null && colorClass != null && !colorClass.trim().isEmpty()) {
            icon.getStyleClass().add(colorClass);
        }
        return icon;
    }

    /**
     * Map icon names to FontAwesome icons.
     * @param iconName Name of the icon
     * @return FontAwesomeIcon enum value
     */
    private FontAwesomeIcon mapIconName(String iconName) {
        // Check cache first
        if (iconCache.containsKey(iconName)) {
            return iconCache.get(iconName);
        }
        
        FontAwesomeIcon faIcon;
        switch (iconName) {
            case "icon-book":
                faIcon = FontAwesomeIcon.BOOK;
                break;
            case "icon-user":
                faIcon = FontAwesomeIcon.USER;
                break;
            case "icon-borrow":
                faIcon = FontAwesomeIcon.EXCHANGE;
                break;
            case "icon-author":
                faIcon = FontAwesomeIcon.PENCIL;
                break;
            case "icon-publisher":
                faIcon = FontAwesomeIcon.BUILDING;
                break;
            case "icon-add":
                faIcon = FontAwesomeIcon.PLUS;
                break;
            case "icon-edit":
                faIcon = FontAwesomeIcon.EDIT;
                break;
            case "icon-delete":
                faIcon = FontAwesomeIcon.TRASH;
                break;
            case "icon-search":
                faIcon = FontAwesomeIcon.SEARCH;
                break;
            case "icon-filter":
                faIcon = FontAwesomeIcon.FILTER;
                break;
            case "icon-settings":
                faIcon = FontAwesomeIcon.COG;
                break;
            case "icon-theme-light":
                faIcon = FontAwesomeIcon.SUN_ALT;
                break;
            case "icon-theme-dark":
                faIcon = FontAwesomeIcon.MOON_ALT;
                break;
            case "icon-globe":
                faIcon = FontAwesomeIcon.GLOBE;
                break;
            case "icon-dashboard":
                faIcon = FontAwesomeIcon.DASHBOARD;
                break;
            case "icon-magazine":
                faIcon = FontAwesomeIcon.BOOK;
                break;
            case "icon-brochure":
                faIcon = FontAwesomeIcon.FILE_TEXT_ALT;
                break;
            default:
                logger.warn("No FontAwesome mapping for icon: {}", iconName);
                return null;
        }
        
        // Cache the mapping
        iconCache.put(iconName, faIcon);
        return faIcon;
    }

    /**
     * Clear the icon cache.
     */
    public void clearCache() {
        iconCache.clear();
        logger.info("Icon cache cleared");
    }

    /**
     * Get the current cache size.
     * @return Number of cached icon mappings
     */
    public int getCacheSize() {
        return iconCache.size();
    }
}
