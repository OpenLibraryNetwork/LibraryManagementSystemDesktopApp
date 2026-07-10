package net.gizmolab.library.librarymanagementsystemdesktop.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import net.jqwik.api.*;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fix-checking tests for SearchableComboBoxHelper (Property 1).
 *
 * These tests verify that the FIXED implementation correctly captures items
 * added via getItems().add() / getItems().addAll() after makeSearchable() is
 * called (the async-load pattern used by all controllers).
 *
 * All tests in this class are EXPECTED TO PASS on the fixed code.
 *
 * Bug spec: .kiro/specs/dropdown-search-filter-bug/
 * Validates: Requirements 2.1, 2.2, 2.3 (Property 1 — Fault Condition Fix)
 */
class SearchableComboBoxHelperFixCheckingTest {

    // -----------------------------------------------------------------------
    // JavaFX toolkit initialisation
    // -----------------------------------------------------------------------

    // Static initializer ensures the toolkit is started once, regardless of
    // whether the test is run by JUnit5 or jqwik (which uses its own thread).
    static {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            // Toolkit already running — fine
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        // No-op: toolkit is started in the static initializer above.
        // Kept for compatibility with JUnit5 lifecycle.
    }

    /** Runs a block on the JavaFX Application Thread and waits for completion. */
    private static void runOnFXThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX action timed out");
    }

    // -----------------------------------------------------------------------
    // Property 1 — PBT: async-added items are filterable
    //
    // For random item lists added via getItems().add() and random queries,
    // the filtered result must equal filterItems(allItems, mapper, query).
    //
    // Validates: Requirements 2.1, 2.3
    // -----------------------------------------------------------------------

    /**
     * Property 1 (fix-checking): for any non-empty list of strings added via
     * getItems().add() after makeSearchable(), and any query string, the items
     * visible in the ComboBox after typing the query must equal
     * filterItems(allItems, mapper, query).
     *
     * Uses jqwik for property-based generation. The test runs on the FX thread
     * via a CountDownLatch because jqwik runs on a non-FX thread.
     *
     * Validates: Requirements 2.1, 2.3
     */
    @Property(tries = 20)
    @Label("Property 1: async-added items filtered correctly for any list and query")
    void asyncAddedItems_filteredResultMatchesFilterItems(
            @ForAll("nonEmptyAlphaLists") List<String> items,
            @ForAll("shortQueries") String query) throws InterruptedException {

        AtomicReference<List<String>> actualFiltered = new AtomicReference<>();
        AtomicReference<List<String>> expectedFiltered = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            // Simulate async loading: add items one-by-one via getItems().add()
            for (String item : items) {
                comboBox.getItems().add(item);
            }

            // Capture allItems snapshot (what the fix should have stored)
            List<String> allItemsSnapshot = List.copyOf(comboBox.getItems());

            // Simulate typing the query
            comboBox.getEditor().setText(query);

            actualFiltered.set(List.copyOf(comboBox.getItems()));
            expectedFiltered.set(SearchableComboBoxHelper.filterItems(allItemsSnapshot, s -> s, query));
        });

        // Compare contents — avoid List-type mismatch (ArrayList vs ImmutableList)
        assertEquals(expectedFiltered.get().size(), actualFiltered.get().size(),
                "Filtered size must match filterItems() for items=" + items + ", query='" + query + "'");
        assertTrue(actualFiltered.get().containsAll(expectedFiltered.get()),
                "Filtered ComboBox items must equal filterItems(allItems, mapper, query) " +
                "for items=" + items + ", query='" + query + "'");
    }

    @Provide
    Arbitrary<List<String>> nonEmptyAlphaLists() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12)
                .list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<String> shortQueries() {
        return Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(6);
    }

    // -----------------------------------------------------------------------
    // Unit test 3.2 — clearing query after async-add restores full list
    //
    // Validates: Requirements 2.2, 2.3
    // -----------------------------------------------------------------------

    /**
     * Validates: Requirements 2.2, 2.3
     *
     * After makeSearchable(), items are added via getItems().add(), a query is
     * typed, then the editor is cleared. The full original list must be restored.
     */
    @Test
    void asyncAddThenClearQuery_restoresFullList() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.getItems().add("Alice");
            comboBox.getItems().add("Bob");
            comboBox.getItems().add("Charlie");

            // Type a query that narrows the list
            comboBox.getEditor().setText("ali");
            // Clear the query — full list must be restored
            comboBox.getEditor().setText("");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        List<String> restored = capturedItems.get();
        assertEquals(3, restored.size(),
                "Clearing the query must restore all 3 items.");
        assertTrue(restored.containsAll(List.of("Alice", "Bob", "Charlie")),
                "Restored list must contain all original items.");
    }

    /**
     * Validates: Requirements 2.2, 2.3
     *
     * Same as above but items are loaded via getItems().addAll() — the batch
     * variant used by BookFormController and BorrowFormControllerNew.
     */
    @Test
    void addAllThenClearQuery_restoresFullList() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.getItems().addAll(
                    FXCollections.observableArrayList("Tolkien", "Tolstoy", "Twain", "Hemingway")
            );

            comboBox.getEditor().setText("tol");
            comboBox.getEditor().setText("");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        List<String> restored = capturedItems.get();
        assertEquals(4, restored.size(),
                "Clearing the query must restore all 4 items.");
        assertTrue(restored.containsAll(List.of("Tolkien", "Tolstoy", "Twain", "Hemingway")),
                "Restored list must contain all original items.");
    }

    /**
     * Validates: Requirements 2.1, 2.3
     *
     * Edge case: multiple sequential add() calls followed by a query that
     * matches only a strict subset. Verifies the subset is exact.
     */
    @Test
    void asyncAdd_queryMatchesStrictSubset() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.getItems().add("Tolkien");
            comboBox.getItems().add("Tolstoy");
            comboBox.getItems().add("Twain");
            comboBox.getItems().add("Hemingway");

            comboBox.getEditor().setText("tol");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        List<String> filtered = capturedItems.get();
        assertEquals(2, filtered.size(), "Only 'Tolkien' and 'Tolstoy' match 'tol'.");
        assertTrue(filtered.containsAll(List.of("Tolkien", "Tolstoy")));
        assertFalse(filtered.contains("Twain"));
        assertFalse(filtered.contains("Hemingway"));
    }

    /**
     * Validates: Requirements 2.1, 2.3
     *
     * Edge case: query with no matches returns an empty list (not the full list).
     */
    @Test
    void asyncAdd_queryWithNoMatches_returnsEmptyList() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.getItems().add("Alice");
            comboBox.getItems().add("Bob");

            comboBox.getEditor().setText("zzz");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        assertTrue(capturedItems.get().isEmpty(),
                "A query with no matches must return an empty list.");
    }
}
