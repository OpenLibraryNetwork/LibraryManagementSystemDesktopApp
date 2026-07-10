package net.gizmolab.library.librarymanagementsystemdesktop.util;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PopularBookEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DashboardDataHelper.computePopularBooks method.
 * Validates: Requirements 5.1, 5.2, 5.5, 5.6
 */
@DisplayName("DashboardDataHelper.computePopularBooks")
class DashboardDataHelperComputePopularBooksTest {

    // --- Helper to create a BorrowDTO with title and author ---
    private BorrowDTO createBorrow(String bookTitle, String authorName) {
        BorrowDTO dto = new BorrowDTO();
        dto.setBookTitle(bookTitle);
        dto.setAuthorName(authorName);
        return dto;
    }

    // --- Empty/Null input tests ---

    @Test
    @DisplayName("empty list returns empty result")
    void emptyListReturnsEmpty() {
        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(Collections.emptyList(), 10);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("null list returns empty result")
    void nullListReturnsEmpty() {
        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(null, 10);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- Single book test ---

    @Test
    @DisplayName("single book returns one entry with count 1")
    void singleBookReturnsOneEntry() {
        List<BorrowDTO> borrows = List.of(createBorrow("Clean Code", "Robert Martin"));

        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(borrows, 10);

        assertEquals(1, result.size());
        assertEquals("Clean Code", result.get(0).title());
        assertEquals("Robert Martin", result.get(0).authorName());
        assertEquals(1, result.get(0).borrowCount());
    }

    // --- Multiple books sorted descending by count ---

    @Test
    @DisplayName("multiple books are sorted descending by borrow count")
    void multipleBooksAreSortedDescending() {
        List<BorrowDTO> borrows = new ArrayList<>();
        // "Book A" borrowed 2 times
        borrows.add(createBorrow("Book A", "Author A"));
        borrows.add(createBorrow("Book A", "Author A"));
        // "Book B" borrowed 5 times
        for (int i = 0; i < 5; i++) {
            borrows.add(createBorrow("Book B", "Author B"));
        }
        // "Book C" borrowed 3 times
        for (int i = 0; i < 3; i++) {
            borrows.add(createBorrow("Book C", "Author C"));
        }

        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(borrows, 10);

        assertEquals(3, result.size());
        assertEquals("Book B", result.get(0).title());
        assertEquals(5, result.get(0).borrowCount());
        assertEquals("Book C", result.get(1).title());
        assertEquals(3, result.get(1).borrowCount());
        assertEquals("Book A", result.get(2).title());
        assertEquals(2, result.get(2).borrowCount());
    }

    // --- maxResults limit ---

    @Test
    @DisplayName("maxResults limits the number of returned entries")
    void maxResultsLimitsOutput() {
        List<BorrowDTO> borrows = new ArrayList<>();
        // 3 distinct books
        for (int i = 0; i < 5; i++) {
            borrows.add(createBorrow("Book A", "Author A"));
        }
        for (int i = 0; i < 3; i++) {
            borrows.add(createBorrow("Book B", "Author B"));
        }
        borrows.add(createBorrow("Book C", "Author C"));

        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(borrows, 2);

        assertEquals(2, result.size());
        assertEquals("Book A", result.get(0).title());
        assertEquals(5, result.get(0).borrowCount());
        assertEquals("Book B", result.get(1).title());
        assertEquals(3, result.get(1).borrowCount());
    }

    // --- Null bookTitle entries are skipped ---

    @Test
    @DisplayName("null bookTitle entries are skipped")
    void nullBookTitleEntriesAreSkipped() {
        List<BorrowDTO> borrows = new ArrayList<>();
        borrows.add(createBorrow(null, "Author X"));
        borrows.add(createBorrow(null, null));
        borrows.add(createBorrow("Valid Book", "Valid Author"));

        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(borrows, 10);

        assertEquals(1, result.size());
        assertEquals("Valid Book", result.get(0).title());
        assertEquals(1, result.get(0).borrowCount());
    }

    @Test
    @DisplayName("all null bookTitle entries result in empty output")
    void allNullBookTitleReturnsEmpty() {
        List<BorrowDTO> borrows = new ArrayList<>();
        borrows.add(createBorrow(null, "Author A"));
        borrows.add(createBorrow(null, "Author B"));

        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(borrows, 10);

        assertTrue(result.isEmpty());
    }

    // --- Ties in count ---

    @Test
    @DisplayName("ties in borrow count are handled - both entries present")
    void tiesInCountAreHandled() {
        List<BorrowDTO> borrows = new ArrayList<>();
        // "Book A" borrowed 3 times
        for (int i = 0; i < 3; i++) {
            borrows.add(createBorrow("Book A", "Author A"));
        }
        // "Book B" borrowed 3 times (same count as A)
        for (int i = 0; i < 3; i++) {
            borrows.add(createBorrow("Book B", "Author B"));
        }
        // "Book C" borrowed 1 time (lower)
        borrows.add(createBorrow("Book C", "Author C"));

        List<PopularBookEntry> result = DashboardDataHelper.computePopularBooks(borrows, 10);

        assertEquals(3, result.size());

        // The first two entries should both have count 3 (tied), order doesn't matter between them
        Set<String> topTwoTitles = result.subList(0, 2).stream()
                .map(PopularBookEntry::title)
                .collect(Collectors.toSet());
        assertTrue(topTwoTitles.contains("Book A"));
        assertTrue(topTwoTitles.contains("Book B"));
        assertEquals(3, result.get(0).borrowCount());
        assertEquals(3, result.get(1).borrowCount());

        // The last entry should be Book C with count 1
        assertEquals("Book C", result.get(2).title());
        assertEquals(1, result.get(2).borrowCount());
    }
}
