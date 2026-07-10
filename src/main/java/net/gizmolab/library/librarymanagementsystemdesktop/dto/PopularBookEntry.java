package net.gizmolab.library.librarymanagementsystemdesktop.dto;

/**
 * Record representing a popular book entry in the dashboard.
 *
 * @param title       publication title
 * @param authorName  author name (from first borrow record)
 * @param borrowCount total number of borrows
 */
public record PopularBookEntry(String title, String authorName, long borrowCount) {
}
