package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.service.theme.ThemeManager;
import javafx.application.Application;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeDebugTest {

    @Test
    void testUserAgentStylesheetIsSet() {
        // Simulate what happens in the application
        ThemeManager.getInstance().loadSavedTheme();
        
        // Check what User Agent Stylesheet is set
        String userAgentStylesheet = Application.getUserAgentStylesheet();
        
        System.out.println("=== THEME DEBUG INFO ===");
        System.out.println("User Agent Stylesheet: " + userAgentStylesheet);
        System.out.println("Current Theme: " + ThemeManager.getInstance().getCurrentTheme());
        System.out.println("Expected: /atlantafx/base/theme/primer-light.css");
        System.out.println("========================");
        
        assertNotNull(userAgentStylesheet, "User Agent Stylesheet should be set");
        assertTrue(userAgentStylesheet.contains("atlantafx"), 
            "User Agent Stylesheet should contain 'atlantafx' but was: " + userAgentStylesheet);
    }
}
