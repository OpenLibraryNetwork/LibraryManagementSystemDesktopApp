package net.gizmolab.library.librarymanagementsystemdesktop.util;

/**
 * Encapsulates pagination state for table views.
 * Handles edge cases: empty data, invalid pageSize, out-of-range currentPage.
 */
public class PaginationState {

    private static final int DEFAULT_PAGE_SIZE = 15;

    private int currentPage;
    private int totalPages;
    private final int pageSize;

    /**
     * Creates a new PaginationState.
     *
     * @param currentPage the initial current page (1-based)
     * @param totalPages  the total number of pages
     * @param pageSize    the number of records per page
     */
    public PaginationState(int currentPage, int totalPages, int pageSize) {
        // Fallback for invalid pageSize
        this.pageSize = (pageSize <= 0) ? DEFAULT_PAGE_SIZE : pageSize;

        // Empty data or invalid totalPages → totalPages=1, currentPage=1
        if (totalPages <= 0) {
            this.totalPages = 1;
            this.currentPage = 1;
            return;
        }

        this.totalPages = totalPages;

        // Clamp currentPage to valid range
        if (currentPage < 1) {
            this.currentPage = 1;
        } else if (currentPage > totalPages) {
            this.currentPage = 1;
        } else {
            this.currentPage = currentPage;
        }
    }

    /**
     * Returns true when the previous button should be disabled (on first page).
     */
    public boolean isPreviousDisabled() {
        return currentPage == 1;
    }

    /**
     * Returns true when the next button should be disabled (on last page).
     */
    public boolean isNextDisabled() {
        return currentPage == totalPages;
    }

    /**
     * Navigates to the given page if it is within valid bounds.
     *
     * @param page target page (1-based)
     */
    public void goToPage(int page) {
        if (page >= 1 && page <= totalPages) {
            this.currentPage = page;
        }
    }

    /** Returns the current page (1-based). */
    public int getCurrentPage() {
        return currentPage;
    }

    /** Returns the total number of pages. */
    public int getTotalPages() {
        return totalPages;
    }

    /** Returns the page size (records per page). */
    public int getPageSize() {
        return pageSize;
    }
}
