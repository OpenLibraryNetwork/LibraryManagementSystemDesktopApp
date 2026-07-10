package net.gizmolab.library.librarymanagementsystemdesktop.service.theme;

/**
 * Exception thrown when theme loading fails.
 */
public class ThemeLoadException extends Exception {
    
    public ThemeLoadException(String message) {
        super(message);
    }
    
    public ThemeLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
