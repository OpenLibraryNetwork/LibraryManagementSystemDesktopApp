package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.service.theme.AtlantaFXTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtlantaFXThemeTest {

    @Test
    void testAtlantaFXThemeEnumExists() {
        AtlantaFXTheme[] themes = AtlantaFXTheme.values();
        assertEquals(4, themes.length, "Should have 4 themes");
    }

    @Test
    void testPrimerLightTheme() {
        AtlantaFXTheme theme = AtlantaFXTheme.PRIMER_LIGHT;
        assertNotNull(theme);
        assertEquals("Primer Light", theme.getDisplayName());
        assertFalse(theme.isDark());
        
        String stylesheet = theme.getUserAgentStylesheet();
        assertNotNull(stylesheet, "Stylesheet should not be null");
        System.out.println("PrimerLight stylesheet: " + stylesheet);
    }

    @Test
    void testAllThemesHaveStylesheets() {
        for (AtlantaFXTheme theme : AtlantaFXTheme.values()) {
            String stylesheet = theme.getUserAgentStylesheet();
            assertNotNull(stylesheet, theme.getDisplayName() + " should have a stylesheet");
            assertFalse(stylesheet.isEmpty(), theme.getDisplayName() + " stylesheet should not be empty");
            System.out.println(theme.getDisplayName() + " -> " + stylesheet);
        }
    }
}
