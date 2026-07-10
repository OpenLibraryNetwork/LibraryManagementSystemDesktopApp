package net.gizmolab.library.librarymanagementsystemdesktop.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for DashboardDataHelper.formatCount method.
 * Validates: Requirements 1.2, 2.2, 3.2, 4.2
 */
class DashboardDataHelperFormatCountTest {

    @BeforeAll
    static void setLocale() {
        // Ensure deterministic formatting with comma as thousands separator
        Locale.setDefault(Locale.US);
    }

    @Test
    @DisplayName("formatCount(0) returns \"0\"")
    void formatCount_zero_returnsZero() {
        assertEquals("0", DashboardDataHelper.formatCount(0));
    }

    @Test
    @DisplayName("formatCount(1) returns \"1\"")
    void formatCount_one_returnsOne() {
        assertEquals("1", DashboardDataHelper.formatCount(1));
    }

    @Test
    @DisplayName("formatCount(999) returns \"999\" (no separator needed)")
    void formatCount_999_returnsNoSeparator() {
        assertEquals("999", DashboardDataHelper.formatCount(999));
    }

    @Test
    @DisplayName("formatCount(1000) returns \"1,000\"")
    void formatCount_1000_returnsWithSeparator() {
        assertEquals("1,000", DashboardDataHelper.formatCount(1000));
    }

    @Test
    @DisplayName("formatCount(1000000) returns \"1,000,000\"")
    void formatCount_oneMillion_returnsWithSeparators() {
        assertEquals("1,000,000", DashboardDataHelper.formatCount(1000000));
    }
}
