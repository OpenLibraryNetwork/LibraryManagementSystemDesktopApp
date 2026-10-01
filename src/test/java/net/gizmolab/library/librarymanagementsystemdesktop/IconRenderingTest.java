package net.gizmolab.library.librarymanagementsystemdesktop;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IconProvider;
import net.gizmolab.library.librarymanagementsystemdesktop.testsupport.FxToolkit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Every icon the sidebar uses must render with the icon font (not a fallback font or the "unknown" circle). */
class IconRenderingTest {

    private static final List<String> USED = List.of("icon-author", "icon-book", "icon-borrow", "icon-brochure",
            "icon-dashboard", "icon-globe", "icon-magazine", "icon-publisher", "icon-user");

    @BeforeAll
    static void startFx() throws InterruptedException {
        FxToolkit.start();
    }

    @Test
    void everyUsedIconRendersWithTheIconFont() throws Exception {
        CompletableFuture<List<String>> problems = new CompletableFuture<>();
        Platform.runLater(() -> {
            List<String> found = new ArrayList<>();
            String unknown = glyph(IconProvider.getInstance().getIcon("no-such-icon", 16));
            for (String name : USED) {
                Node icon = IconProvider.getInstance().getIcon(name, 16);
                new Scene(new Group(icon)).getRoot().applyCss();
                String family = ((Text) icon).getFont().getFamily();
                if (!family.toLowerCase().contains("fontawesome")) found.add(name + ": font " + family);
                if (glyph(icon).equals(unknown)) found.add(name + ": falls back to the unknown icon");
            }
            problems.complete(found);
        });
        assertEquals(List.of(), problems.get(10, TimeUnit.SECONDS));
    }

    private static String glyph(Node icon) {
        return ((Text) icon).getText();
    }
}
