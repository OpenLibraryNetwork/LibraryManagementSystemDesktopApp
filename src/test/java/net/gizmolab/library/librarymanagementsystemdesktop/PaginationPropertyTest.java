package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.util.PaginationHelper;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests and unit tests for PaginationHelper.
 *
 * Feature: table-modernization
 */
class PaginationPropertyTest {

    // -----------------------------------------------------------------------
    // Property 2: Page window contains current ± 2 and boundaries
    // Feature: table-modernization, Property 2: Page window contains current ± 2 and boundaries
    // Validates: Requirements 6.1, 6.5
    // -----------------------------------------------------------------------

    /**
     * **Validates: Απαιτήσεις 6.1, 6.5**
     *
     * For any totalPages and currentPage, the visible pages must include
     * pages max(1, currentPage-2) through min(totalPages, currentPage+2),
     * plus always page 1 and totalPages.
     */
    @Property(tries = 30)
    @Label("Property 2: Page window contains current ± 2 and boundaries")
    void paginationWindowContainsExpectedPages(
            @ForAll @IntRange(min = 1, max = 50) int totalPages,
            @ForAll @IntRange(min = 1, max = 50) int currentPage) {
        Assume.that(currentPage <= totalPages);

        List<Integer> visiblePages = PaginationHelper.buildVisiblePages(currentPage, totalPages);

        // Window pages must all be present
        int low = Math.max(1, currentPage - 2);
        int high = Math.min(totalPages, currentPage + 2);
        for (int p = low; p <= high; p++) {
            assertTrue(visiblePages.contains(p),
                    "Visible pages must contain page " + p + " (window for currentPage=" + currentPage + ")");
        }

        // Boundary pages must always be present
        assertTrue(visiblePages.contains(1), "Visible pages must always contain page 1");
        assertTrue(visiblePages.contains(totalPages), "Visible pages must always contain last page");
    }

    // -----------------------------------------------------------------------
    // Unit test 4.3: BaseManagementController has pagination fields and methods
    // Validates: Requirements 8.2
    // -----------------------------------------------------------------------

    /**
     * Verifies that BaseManagementController declares the expected pagination
     * fields and methods via reflection (no JavaFX runtime needed).
     *
     * Validates: Απαιτήσεις 8.2
     */
    @Test
    void testPaginationBarHasNavButtons() throws Exception {
        Class<?> clazz = Class.forName(
                "net.gizmolab.library.librarymanagementsystemdesktop.controller.base.BaseManagementController");

        // Fields
        assertDoesNotThrow(() -> clazz.getDeclaredField("pageButtonsContainer"),
                "BaseManagementController must declare pageButtonsContainer");
        assertDoesNotThrow(() -> clazz.getDeclaredField("previousButton"),
                "BaseManagementController must declare previousButton");
        assertDoesNotThrow(() -> clazz.getDeclaredField("nextButton"),
                "BaseManagementController must declare nextButton");
        assertDoesNotThrow(() -> clazz.getDeclaredField("currentPage"),
                "BaseManagementController must declare currentPage");
        assertDoesNotThrow(() -> clazz.getDeclaredField("pageSize"),
                "BaseManagementController must declare pageSize");
        assertDoesNotThrow(() -> clazz.getDeclaredField("totalPages"),
                "BaseManagementController must declare totalPages");

        // Methods
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("setupPagination"),
                "BaseManagementController must declare setupPagination()");
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("updatePaginationBar"),
                "BaseManagementController must declare updatePaginationBar()");
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("buildPageButtons"),
                "BaseManagementController must declare buildPageButtons()");
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("goToPage", int.class),
                "BaseManagementController must declare goToPage(int)");
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("getCurrentPageData"),
                "BaseManagementController must declare getCurrentPageData()");
    }
}
