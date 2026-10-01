package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The sidebar is loaded once; a language change re-sets its texts in MainNavigationController.refreshUI().
 * Every element of main-navigation.fxml with a translated text must be re-set there, or it stays in the old language.
 */
class MainNavigationLanguageRefreshTest {

    // Re-set by updateStatus() / updateConnectionStatus(), not by a fixed key
    private static final Set<String> STATUS_LABELS = Set.of("statusLabel", "connectionStatusLabel");

    @Test
    void everyTranslatedSidebarTextIsRefreshedOnLanguageChange() throws IOException {
        String fxml = Files.readString(Path.of("src/main/resources/fxml/main-navigation.fxml"));
        String controller = Files.readString(Path.of(
                "src/main/java/net/gizmolab/library/librarymanagementsystemdesktop/controller/MainNavigationController.java"));

        List<String> missing = new ArrayList<>();
        Matcher m = Pattern.compile("fx:id=\"(\\w+)\"[^>]*?text=\"%([\\w.]+)\"").matcher(fxml);
        while (m.find()) {
            String id = m.group(1);
            String key = m.group(2);
            if (STATUS_LABELS.contains(id)) continue;
            if (!controller.contains(id + ".setText(i18nManager.getMessage(\"" + key + "\"))")) missing.add(id + " → " + key);
        }
        assertEquals(List.of(), missing);
    }
}
