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
 * Preservation-checking tests for SearchableComboBoxHelper (Property 2).
 *
 * These tests verify that the FIXED implementation does NOT break any behaviour
 * that already worked correctly before the fix. All tests in this class are
 * EXPECTED TO PASS on both the original and the fixed code (for the paths that
 * were never broken), confirming that the fix is non-regressive.
 *
 * Covered preservation scenarios:
 *   4.1 setItems() path — items loaded via setItems() still filter correctly
 *   4.2 Item selection — selecting an item still sets editor text and restores full list
 *   4.3 Guard clause — editor text == selected item display string skips filtering
 *   4.4 filterItems utility — static method returns identical results (unchanged)
 *
 * Bug spec: .kiro/specs/dropdown-search-filter-bug/
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5 (Property 2 — Preservation)
 */
class SearchableComboBoxHelperPreservationTest {

    // -----------------------------------------------------------------------
    // JavaFX toolkit initialisation
    // -----------------------------------------------------------------------

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
    static void initJavaFX() {
        // No-op: toolkit started in static initializer above.
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
    // 4.1 — setItems() path still populates allItems and filters correctly
    //
    // The itemsProperty ChangeListener was removed by the fix, but the eager
    // seed (allItems.setAll(comboBox.getItems())) handles items already present
    // at makeSearchable() call time. Items loaded via setItems() BEFORE
    // makeSearchable() is called must still be captured and filterable.
    //
    // Validates: Requirements 3.3, 3.4, 3.5
    // -----------------------------------------------------------------------

    /**
     * Items loaded via setItems() BEFORE makeSearchable() is called must be
     * captured by the eager seed and remain filterable.
     *
     * Validates: Requirements 3.3, 3.4
     */
    @Test
    void setItemsBeforeMakeSearchable_filtersCorrectly() throws InterruptedException {
        AtomicReference<List<String>> filtered = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            // Load items BEFORE makeSearchable — the eager seed must capture them
            comboBox.setItems(FXCollections.observableArrayList("Alice", "Bob", "Charlie"));
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.getEditor().setText("ali");
            filtered.set(List.copyOf(comboBox.getItems()));
        });

        assertEquals(1, filtered.get().size(), "Only 'Alice' matches 'ali'.");
        assertTrue(filtered.get().contains("Alice"));
    }

    /**
     * Items loaded via setItems() BEFORE makeSearchable() — clearing the query
     * must restore the full list.
     *
     * Validates: Requirements 3.3, 3.4
     */
    @Test
    void setItemsBeforeMakeSearchable_clearQueryRestoresFullList() throws InterruptedException {
        AtomicReference<List<String>> restored = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(FXCollections.observableArrayList("Alice", "Bob", "Charlie"));
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.getEditor().setText("ali");
            comboBox.getEditor().setText("");
            restored.set(List.copyOf(comboBox.getItems()));
        });

        assertEquals(3, restored.get().size(), "Full list of 3 items must be restored.");
        assertTrue(restored.get().containsAll(List.of("Alice", "Bob", "Charlie")));
    }

    /**
     * Property 2 (preservation PBT): for any list loaded via setItems() and any
     * query, the filtered result must equal filterItems(allItems, mapper, query).
     *
     * Validates: Requirements 3.3, 3.4, 3.5
     */
    @Property(tries = 20)
    @Label("Property 2: setItems() path filters identically to filterItems() for any list and query")
    void setItemsPath_filteredResultMatchesFilterItems(
            @ForAll("nonEmptyAlphaLists") List<String> items,
            @ForAll("shortQueries") String query) throws InterruptedException {

        AtomicReference<List<String>> actual = new AtomicReference<>();
        AtomicReference<List<String>> expected = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(FXCollections.observableArrayList(items));
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            List<String> snapshot = List.copyOf(comboBox.getItems());
            comboBox.getEditor().setText(query);

            actual.set(List.copyOf(comboBox.getItems()));
            expected.set(SearchableComboBoxHelper.filterItems(snapshot, s -> s, query));
        });

        assertEquals(expected.get().size(), actual.get().size(),
                "setItems() path: filtered size must match filterItems() for query='" + query + "'");
        assertTrue(actual.get().containsAll(expected.get()),
                "setItems() path: filtered items must match filterItems() result");
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
    // 4.2 — Item selection still sets editor text and restores full list
    //
    // The valueProperty listener uses Platform.runLater to set editor text and
    // restore allItems. This must be unaffected by the fix.
    //
    // Validates: Requirements 3.1
    // -----------------------------------------------------------------------

    /**
     * Selecting an item must set the editor text to the item's display string.
     * The value listener uses Platform.runLater, so we schedule the assertion
     * after the runLater has had a chance to execute.
     *
     * Validates: Requirement 3.1
     */
    @Test
    void itemSelection_setsEditorTextToDisplayString() throws InterruptedException {
        AtomicReference<String> editorText = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll("Alice", "Bob", "Charlie");
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.setValue("Bob");

            // Schedule capture AFTER the value listener's own Platform.runLater
            Platform.runLater(() -> {
                editorText.set(comboBox.getEditor().getText());
                latch.countDown();
            });
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Selection runLater timed out");
        assertEquals("Bob", editorText.get(),
                "Editor text must be set to the selected item's display string.");
    }

    /**
     * After selecting an item, the full list must be restored (via Platform.runLater).
     * We verify this by checking the ComboBox items after the FX queue drains.
     *
     * Validates: Requirement 3.1
     */
    @Test
    void itemSelection_restoresFullListAfterSelection() throws InterruptedException {
        AtomicReference<List<String>> itemsAfterSelection = new AtomicReference<>();

        // Run setup and selection on FX thread
        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll("Alice", "Bob", "Charlie");
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            // Type a query to narrow the list first
            comboBox.getEditor().setText("ali");
            // Now select an item — valueProperty listener should restore full list via runLater
            comboBox.setValue("Alice");
        });

        // Drain the FX queue so Platform.runLater inside the value listener executes
        runOnFXThread(() -> {
            // no-op drain
        });

        // Capture items after the runLater has fired
        runOnFXThread(() -> {
            // We need a fresh reference — re-run the scenario and capture after drain
        });

        // Verify via a self-contained scenario that captures after the runLater fires
        AtomicReference<List<String>> capturedAfterRunLater = new AtomicReference<>();
        CountDownLatch selectionLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll("Alice", "Bob", "Charlie");
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.getEditor().setText("ali");
            comboBox.setValue("Alice");

            // Schedule capture AFTER the value listener's runLater
            Platform.runLater(() -> {
                capturedAfterRunLater.set(List.copyOf(comboBox.getItems()));
                selectionLatch.countDown();
            });
        });

        assertTrue(selectionLatch.await(5, TimeUnit.SECONDS), "Selection runLater timed out");

        List<String> items = capturedAfterRunLater.get();
        assertEquals(3, items.size(),
                "Full list of 3 items must be restored after item selection.");
        assertTrue(items.containsAll(List.of("Alice", "Bob", "Charlie")),
                "All original items must be present after selection.");
    }

    // -----------------------------------------------------------------------
    // 4.3 — Guard clause still skips filtering when editor text equals
    //        selected item's display string
    //
    // When the user selects an item, the value listener sets the editor text to
    // the item's display string via Platform.runLater. This triggers the text
    // listener, which must NOT filter the list (guard clause). Without the guard,
    // the list would be replaced with a filtered subset on every selection.
    //
    // Validates: Requirement 3.2
    // -----------------------------------------------------------------------

    /**
     * When editor text equals the selected item's display string, the guard
     * clause must prevent filtering — the full list must remain intact.
     *
     * Validates: Requirement 3.2
     */
    @Test
    void guardClause_skipsFilteringWhenEditorMatchesSelectedItem() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll("Alice", "Bob", "Charlie");
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            // Set a selected value first
            comboBox.setValue("Alice");

            // Now set editor text to exactly the selected item's display string.
            // The guard clause must detect this and skip filtering.
            comboBox.getEditor().setText("Alice");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        // The guard clause returns early, so the full list must still be present
        // (not filtered to just ["Alice"]).
        List<String> items = capturedItems.get();
        assertEquals(3, items.size(),
                "Guard clause must skip filtering when editor text equals selected item. " +
                "Full list of 3 items must remain.");
        assertTrue(items.containsAll(List.of("Alice", "Bob", "Charlie")),
                "All items must be present — guard clause prevented filtering.");
    }

    /**
     * Guard clause must NOT skip filtering when editor text differs from the
     * selected item's display string (normal search scenario).
     *
     * Validates: Requirement 3.2 (negative case — guard does not over-fire)
     */
    @Test
    void guardClause_doesNotSkipFilteringForDifferentText() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.getItems().addAll("Alice", "Bob", "Charlie");
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            comboBox.setValue("Alice");

            // Type something different from the selected item — guard must NOT fire
            comboBox.getEditor().setText("bob");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        List<String> items = capturedItems.get();
        assertEquals(1, items.size(),
                "Guard clause must not fire for text 'bob' when selected item is 'Alice'. " +
                "Only 'Bob' should be in the filtered list.");
        assertTrue(items.contains("Bob"),
                "Filtered list must contain 'Bob' for query 'bob'.");
    }

    // -----------------------------------------------------------------------
    // 4.4 — filterItems static utility method returns identical results
    //
    // filterItems() is not modified by the fix. These tests confirm it behaves
    // identically to what the fix-checking and exploratory tests relied on.
    //
    // Validates: Requirement 3.3
    // -----------------------------------------------------------------------

    /**
     * filterItems() with a matching query returns only matching items (case-insensitive).
     *
     * Validates: Requirement 3.3
     */
    @Test
    void filterItems_returnsMatchingSubset() {
        List<String> items = List.of("Tolkien", "Tolstoy", "Twain", "Hemingway");
        List<String> result = SearchableComboBoxHelper.filterItems(items, s -> s, "tol");

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of("Tolkien", "Tolstoy")));
        assertFalse(result.contains("Twain"));
        assertFalse(result.contains("Hemingway"));
    }

    /**
     * filterItems() with an empty query returns the full list (copy).
     *
     * Validates: Requirement 3.3
     */
    @Test
    void filterItems_emptyQuery_returnsFullList() {
        List<String> items = List.of("Alice", "Bob", "Charlie");
        List<String> result = SearchableComboBoxHelper.filterItems(items, s -> s, "");

        assertEquals(3, result.size());
        assertTrue(result.containsAll(items));
    }

    /**
     * filterItems() with null query returns the full list (copy).
     *
     * Validates: Requirement 3.3
     */
    @Test
    void filterItems_nullQuery_returnsFullList() {
        List<String> items = List.of("Alice", "Bob");
        List<String> result = SearchableComboBoxHelper.filterItems(items, s -> s, null);

        assertEquals(2, result.size());
        assertTrue(result.containsAll(items));
    }

    /**
     * filterItems() with a query that matches nothing returns an empty list.
     *
     * Validates: Requirement 3.3
     */
    @Test
    void filterItems_noMatchingItems_returnsEmptyList() {
        List<String> items = List.of("Alice", "Bob", "Charlie");
        List<String> result = SearchableComboBoxHelper.filterItems(items, s -> s, "zzz");

        assertTrue(result.isEmpty(), "No items match 'zzz' — result must be empty.");
    }

    /**
     * filterItems() is case-insensitive: "ALI" must match "Alice".
     *
     * Validates: Requirement 3.3
     */
    @Test
    void filterItems_isCaseInsensitive() {
        List<String> items = List.of("Alice", "Bob");
        List<String> result = SearchableComboBoxHelper.filterItems(items, s -> s, "ALI");

        assertEquals(1, result.size());
        assertTrue(result.contains("Alice"));
    }

    /**
     * filterItems() returns a new list — mutating the result does not affect the
     * original items list.
     *
     * Validates: Requirement 3.3
     */
    @Test
    void filterItems_returnsIndependentCopy() {
        List<String> items = List.of("Alice", "Bob", "Charlie");
        List<String> result = SearchableComboBoxHelper.filterItems(items, s -> s, "");

        // Mutate the result — original must be unaffected
        result.clear();
        assertEquals(3, items.size(), "Original list must not be affected by mutating the result.");
    }

    /**
     * Property 2 (preservation PBT for filterItems): for any list and query,
     * filterItems() must return exactly the items whose display string contains
     * the query (case-insensitive). This is a pure function — no FX thread needed.
     *
     * Validates: Requirement 3.3
     */
    @Property(tries = 20)
    @Label("Property 2: filterItems() always returns exactly the case-insensitive matching subset")
    void filterItems_alwaysReturnsExactMatchingSubset(
            @ForAll("nonEmptyAlphaLists") List<String> items,
            @ForAll("shortQueries") String query) {

        List<String> result = SearchableComboBoxHelper.filterItems(items, s -> s, query);

        String lower = query.toLowerCase();

        // Every item in the result must match the query
        for (String item : result) {
            assertTrue(item.toLowerCase().contains(lower),
                    "Result item '" + item + "' does not match query '" + query + "'");
        }

        // Every matching item in the original list must appear in the result
        for (String item : items) {
            if (item.toLowerCase().contains(lower)) {
                assertTrue(result.contains(item),
                        "Matching item '" + item + "' is missing from filterItems() result");
            }
        }
    }
}
