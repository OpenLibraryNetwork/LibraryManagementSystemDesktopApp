package net.gizmolab.library.librarymanagementsystemdesktop.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exploratory fault-condition tests for SearchableComboBoxHelper.
 *
 * These tests assert CORRECT (fixed) behaviour and are EXPECTED TO FAIL on the
 * unfixed code. Failure confirms the bug: items added via getItems().add() after
 * makeSearchable() is called are never captured into allItems, so filtering
 * returns nothing and clearing the search empties the dropdown.
 *
 * Bug spec: .kiro/specs/dropdown-search-filter-bug/
 * Validates: Requirements 2.1, 2.2, 2.3
 */
class SearchableComboBoxHelperExploratoryTest {

    // -----------------------------------------------------------------------
    // JavaFX toolkit initialisation
    // -----------------------------------------------------------------------

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        // Platform.startup() is a no-op if the toolkit is already running.
        // We use a latch so the test thread waits until the FX thread is ready.
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Toolkit already initialised — that's fine.
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start in time");
    }

    /**
     * Runs a block on the JavaFX Application Thread and waits for it to finish.
     */
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
    // Test 1.1 — async-add then filter returns non-empty results
    //
    // EXPECTED TO FAIL on unfixed code:
    //   allItems is empty because the itemsProperty listener never fired.
    //   Filtering streams over zero elements → filtered list is empty.
    // -----------------------------------------------------------------------

    /**
     * Validates: Requirements 2.1, 2.3
     *
     * Call makeSearchable, then add items via getItems().add() (the async pattern
     * used by all controllers), then type a partial query. The filtered list must
     * be non-empty.
     *
     * FAILS on unfixed code — confirms the bug.
     */
    @Test
    void asyncAddThenFilter_returnsNonEmptyResults() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            // Simulate async loading: items added AFTER makeSearchable via getItems().add()
            comboBox.getItems().add("Alice");
            comboBox.getItems().add("Bob");
            comboBox.getItems().add("Charlie");

            // Simulate typing "ali" in the editor
            comboBox.getEditor().setText("ali");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        List<String> filtered = capturedItems.get();
        assertFalse(filtered.isEmpty(),
                "Filtered list must not be empty — 'ali' should match 'Alice'. " +
                "EXPECTED FAILURE on unfixed code: allItems is empty because " +
                "itemsProperty listener never fired for getItems().add().");
        assertTrue(filtered.contains("Alice"),
                "Filtered list must contain 'Alice' for query 'ali'.");
    }

    // -----------------------------------------------------------------------
    // Test 1.2 — async-add then clear restores full list
    //
    // EXPECTED TO FAIL on unfixed code:
    //   allItems is empty → clearing the editor calls comboBox.setItems(allItems)
    //   which replaces the live list with an empty list.
    // -----------------------------------------------------------------------

    /**
     * Validates: Requirements 2.2, 2.3
     *
     * Call makeSearchable, add items via getItems().add(), type a query, then
     * clear the editor. The full list must be restored.
     *
     * FAILS on unfixed code — confirms the bug.
     */
    @Test
    void asyncAddThenClear_restoresFullList() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            // Simulate async loading
            comboBox.getItems().add("Alice");
            comboBox.getItems().add("Bob");
            comboBox.getItems().add("Charlie");

            // Type a query first, then clear it
            comboBox.getEditor().setText("ali");
            comboBox.getEditor().setText("");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        List<String> restored = capturedItems.get();
        assertEquals(3, restored.size(),
                "After clearing the search, the full list of 3 items must be restored. " +
                "EXPECTED FAILURE on unfixed code: allItems is empty, so clearing the " +
                "editor replaces the live list with an empty list.");
        assertTrue(restored.containsAll(List.of("Alice", "Bob", "Charlie")),
                "Restored list must contain all original items.");
    }

    // -----------------------------------------------------------------------
    // Test 1.3 — addAll variant: filter after addAll returns correct subset
    //
    // EXPECTED TO FAIL on unfixed code:
    //   Same root cause — getItems().addAll() also mutates the list in-place
    //   without replacing the itemsProperty reference.
    // -----------------------------------------------------------------------

    /**
     * Validates: Requirements 2.1, 2.3
     *
     * Call makeSearchable, add items via getItems().addAll() (the pattern used
     * by BookFormController and BorrowFormControllerNew), then type a partial
     * query. The filtered list must contain only matching items.
     *
     * FAILS on unfixed code — confirms the bug.
     */
    @Test
    void addAllVariant_filterAfterAddAllReturnsCorrectSubset() throws InterruptedException {
        AtomicReference<List<String>> capturedItems = new AtomicReference<>();

        runOnFXThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            SearchableComboBoxHelper.makeSearchable(comboBox, s -> s);

            // Simulate async loading via addAll (as used in controllers)
            comboBox.getItems().addAll(
                    FXCollections.observableArrayList("Tolkien", "Tolstoy", "Twain", "Hemingway")
            );

            // Type "tol" — should match "Tolkien" and "Tolstoy"
            comboBox.getEditor().setText("tol");

            capturedItems.set(List.copyOf(comboBox.getItems()));
        });

        List<String> filtered = capturedItems.get();
        assertFalse(filtered.isEmpty(),
                "Filtered list must not be empty — 'tol' should match 'Tolkien' and 'Tolstoy'. " +
                "EXPECTED FAILURE on unfixed code: allItems is empty because " +
                "itemsProperty listener never fired for getItems().addAll().");
        assertEquals(2, filtered.size(),
                "Exactly 2 items should match 'tol': Tolkien and Tolstoy.");
        assertTrue(filtered.containsAll(List.of("Tolkien", "Tolstoy")),
                "Filtered list must contain 'Tolkien' and 'Tolstoy'.");
        assertFalse(filtered.contains("Twain"),
                "'Twain' must not appear in results for query 'tol'.");
        assertFalse(filtered.contains("Hemingway"),
                "'Hemingway' must not appear in results for query 'tol'.");
    }
}
