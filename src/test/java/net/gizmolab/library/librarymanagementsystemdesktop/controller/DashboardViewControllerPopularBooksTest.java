package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardViewController popular books loading.
 * Uses reflection to inject mocked services and invoke private methods
 * without requiring TestFX.
 *
 * Validates: Requirements 5.1, 5.2, 5.3
 */
@DisplayName("DashboardViewController - Popular Books Loading")
class DashboardViewControllerPopularBooksTest {

    private DashboardViewController controller;
    private IBorrowService borrowService;
    private VBox popularBooksContainer;

    @BeforeAll
    static void initToolkit() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new DashboardViewController();
        borrowService = Mockito.mock(IBorrowService.class);
        popularBooksContainer = new VBox();

        // Inject mocked borrowService via reflection
        Field borrowServiceField = DashboardViewController.class.getDeclaredField("borrowService");
        borrowServiceField.setAccessible(true);
        borrowServiceField.set(controller, borrowService);

        // Inject real VBox for popularBooksContainer via reflection
        Field containerField = DashboardViewController.class.getDeclaredField("popularBooksContainer");
        containerField.setAccessible(true);
        containerField.set(controller, popularBooksContainer);
    }

    @Test
    @DisplayName("Popular books container has correct number of rows for 3 distinct books")
    void loadPopularBooks_threeDistinctBooks_createsThreeRows() throws Exception {
        // Arrange: 3 distinct books with different borrow counts
        // "Book A" x3, "Book B" x2, "Book C" x1
        List<BorrowDTO> borrows = Arrays.asList(
                createBorrowDTO("Book A", "Author A"),
                createBorrowDTO("Book A", "Author A"),
                createBorrowDTO("Book A", "Author A"),
                createBorrowDTO("Book B", "Author B"),
                createBorrowDTO("Book B", "Author B"),
                createBorrowDTO("Book C", "Author C")
        );
        when(borrowService.getAllBorrowsAsDTO()).thenReturn(borrows);

        // Act: invoke private loadPopularBooks() method
        invokeLoadPopularBooks();

        // Assert: 3 distinct books => 3 HBox rows
        assertEquals(3, popularBooksContainer.getChildren().size());
    }

    @Test
    @DisplayName("Popular books container is empty when no borrows exist")
    void loadPopularBooks_emptyList_noRows() throws Exception {
        // Arrange: empty borrow list
        when(borrowService.getAllBorrowsAsDTO()).thenReturn(Collections.emptyList());

        // Act
        invokeLoadPopularBooks();

        // Assert: no children
        assertEquals(0, popularBooksContainer.getChildren().size());
    }

    @Test
    @DisplayName("Popular books container is empty when service throws RuntimeException")
    void loadPopularBooks_serviceThrows_noRows() throws Exception {
        // Arrange: service throws exception
        when(borrowService.getAllBorrowsAsDTO()).thenThrow(new RuntimeException("DB connection failed"));

        // Act
        invokeLoadPopularBooks();

        // Assert: graceful failure, container remains empty
        assertEquals(0, popularBooksContainer.getChildren().size());
    }

    // --- Helper methods ---

    private void invokeLoadPopularBooks() throws Exception {
        Method method = DashboardViewController.class.getDeclaredMethod("loadPopularBooks");
        method.setAccessible(true);
        method.invoke(controller);
    }

    private BorrowDTO createBorrowDTO(String bookTitle, String authorName) {
        BorrowDTO dto = new BorrowDTO();
        dto.setBookTitle(bookTitle);
        dto.setAuthorName(authorName);
        return dto;
    }
}
