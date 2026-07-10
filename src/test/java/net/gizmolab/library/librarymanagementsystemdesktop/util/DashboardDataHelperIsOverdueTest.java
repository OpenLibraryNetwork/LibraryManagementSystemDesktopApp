package net.gizmolab.library.librarymanagementsystemdesktop.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DashboardDataHelper.isOverdue(Date borrowDate, Date returnDate).
 * Validates: Requirements 3.1
 */
class DashboardDataHelperIsOverdueTest {

    /**
     * Helper to create a Date that is a given number of days before now.
     */
    private Date daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }

    @Test
    @DisplayName("Borrow date exactly 14 days ago → NOT overdue (due date not yet exceeded)")
    void borrowDateExactly14DaysAgo_shouldNotBeOverdue() {
        Date borrowDate = daysAgo(14);
        boolean result = DashboardDataHelper.isOverdue(borrowDate, null);
        assertFalse(result, "A borrow exactly 14 days ago should NOT be overdue (strictly after 14 days required)");
    }

    @Test
    @DisplayName("Borrow date 15 days ago → overdue (current date is strictly after borrowDate + 14 days)")
    void borrowDate15DaysAgo_shouldBeOverdue() {
        Date borrowDate = daysAgo(15);
        boolean result = DashboardDataHelper.isOverdue(borrowDate, null);
        assertTrue(result, "A borrow 15 days ago should be overdue");
    }

    @Test
    @DisplayName("Borrow date 13 days ago → NOT overdue")
    void borrowDate13DaysAgo_shouldNotBeOverdue() {
        Date borrowDate = daysAgo(13);
        boolean result = DashboardDataHelper.isOverdue(borrowDate, null);
        assertFalse(result, "A borrow 13 days ago should NOT be overdue");
    }

    @Test
    @DisplayName("Null borrow date → returns false")
    void nullBorrowDate_shouldReturnFalse() {
        boolean result = DashboardDataHelper.isOverdue(null, null);
        assertFalse(result, "A null borrow date should return false");
    }

    @Test
    @DisplayName("Non-null return date → always returns false regardless of borrow date")
    void nonNullReturnDate_shouldAlwaysReturnFalse() {
        // Even if the borrow is old enough to be overdue, a non-null return date means it's returned
        Date borrowDate = daysAgo(30); // 30 days ago - would be overdue if not returned
        Date returnDate = daysAgo(1);  // returned yesterday

        boolean result = DashboardDataHelper.isOverdue(borrowDate, returnDate);
        assertFalse(result, "A borrow with a non-null return date should never be overdue");
    }
}
