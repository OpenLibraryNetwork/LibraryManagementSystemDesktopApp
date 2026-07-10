package net.gizmolab.library.librarymanagementsystemdesktop.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Static utility class for pagination calculations.
 */
public final class PaginationHelper {

    private PaginationHelper() {
        // utility class
    }

    /**
     * Builds the list of visible page numbers for a pagination bar.
     *
     * <p>Always includes:
     * <ul>
     *   <li>Page 1</li>
     *   <li>Page {@code totalPages}</li>
     *   <li>All pages in the range [{@code max(1, currentPage-2)}, {@code min(totalPages, currentPage+2)}]</li>
     * </ul>
     *
     * <p>The returned list is sorted and deduplicated.
     *
     * @param currentPage the current page (1-based)
     * @param totalPages  the total number of pages (≥ 1)
     * @return sorted, deduplicated list of visible page numbers
     */
    public static List<Integer> buildVisiblePages(int currentPage, int totalPages) {
        TreeSet<Integer> pages = new TreeSet<>();

        // Always include first and last page
        pages.add(1);
        pages.add(totalPages);

        // Include window: currentPage ± 2, clamped to [1, totalPages]
        int low = Math.max(1, currentPage - 2);
        int high = Math.min(totalPages, currentPage + 2);
        for (int p = low; p <= high; p++) {
            pages.add(p);
        }

        return new ArrayList<>(pages);
    }
}
