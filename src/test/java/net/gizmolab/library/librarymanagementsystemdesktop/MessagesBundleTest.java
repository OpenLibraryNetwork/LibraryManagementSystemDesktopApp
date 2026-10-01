package net.gizmolab.library.librarymanagementsystemdesktop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A text the UI asks for must exist in every language; otherwise the raw key shows on screen. */
class MessagesBundleTest {

    private static final List<String> BUNDLES = List.of("messages.properties", "messages_el.properties", "messages_en.properties");
    /** MainNavigationController builds "navigation." + module name. */
    private static final List<String> MODULES = List.of("dashboard", "publications", "brochures", "users", "borrows",
            "authors", "publishers", "magazines");

    private static Set<String> keys(String bundle) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(Path.of("src/main/resources", bundle), StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return new TreeSet<>(p.stringPropertyNames());
    }

    private static Set<String> matches(Path root, String glob, Pattern pattern) throws IOException {
        Set<String> found = new TreeSet<>();
        var matcher = root.getFileSystem().getPathMatcher("glob:" + glob);
        try (Stream<Path> files = Files.walk(root)) {
            for (Path f : files.filter(f -> matcher.matches(f.getFileName())).toList()) {
                Matcher m = pattern.matcher(Files.readString(f));
                while (m.find()) found.add(m.group(1));
            }
        }
        return found;
    }

    @Test
    void everyLanguageHasTheSameTexts() throws IOException {
        Set<String> base = keys(BUNDLES.get(0));
        for (String bundle : BUNDLES.subList(1, BUNDLES.size())) {
            Set<String> missing = new TreeSet<>(base);
            missing.removeAll(keys(bundle));
            Set<String> extra = new TreeSet<>(keys(bundle));
            extra.removeAll(base);
            assertEquals(Set.of(), missing, "missing in " + bundle);
            assertEquals(Set.of(), extra, "only in " + bundle);
        }
    }

    @Test
    void everyTextTheUiAsksForExists() throws IOException {
        Set<String> used = new TreeSet<>();
        used.addAll(matches(Path.of("src/main/resources/fxml"), "*.fxml", Pattern.compile("\"%([\\w.]+)\"")));
        used.addAll(matches(Path.of("src/main/java"), "*.java", Pattern.compile(
                "(?:getMessage|getLocalizedMessage|updateStatus)\\(\\s*\"([a-z][\\w]*\\.[\\w.]+)\"")));
        MODULES.forEach(m -> used.add("navigation." + m));
        for (String bundle : BUNDLES) {
            Set<String> missing = new TreeSet<>(used);
            missing.removeAll(keys(bundle));
            assertEquals(Set.of(), missing, "used by the UI but missing in " + bundle);
        }
    }
}
