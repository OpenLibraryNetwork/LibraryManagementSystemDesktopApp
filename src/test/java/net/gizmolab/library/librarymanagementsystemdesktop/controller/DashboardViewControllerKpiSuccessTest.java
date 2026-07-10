package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import app.angeasla.librarymanagementsystemdesktop.service.IBookService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DashboardViewController KPI loading success path.
 * Uses reflection to inject mocked services and FXML labels, then verifies
 * that labels are set to correctly formatted values after loadKpiData().
 *
 * Validates: Requirements 1.1, 2.1, 3.1, 4.1
 */
@ExtendWith(MockitoExtension.class)
class DashboardViewControllerKpiSuccessTest {

    @Mock
    private IBookService bookService;

    @Mock
    private IBorrowService borrowService;

    @Mock
    private IUserService userService;

    private DashboardViewController controller;

    private Label totalBooksValue;
    private Label activeLoansValue;
    private Label overdueValue;
    private Label registeredMembersValue;

    @BeforeAll
    static void initToolkit() {
        Locale.setDefault(Locale.US);
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new DashboardViewController();

        // Create real JavaFX Label instances
        totalBooksValue = new Label();
        activeLoansValue = new Label();
        overdueValue = new Label();
        registeredMembersValue = new Label();

        // Inject mocked services via reflection
        setField(controller, "bookService", bookService);
        setField(controller, "borrowService", borrowService);
        setField(controller, "userService", userService);

        // Inject FXML labels via reflection
        setField(controller, "totalBooksValue", totalBooksValue);
        setField(controller, "activeLoansValue", activeLoansValue);
        setField(controller, "overdueValue", overdueValue);
        setField(controller, "registeredMembersValue", registeredMembersValue);
    }

    @Test
    void loadKpiData_setsLabelsToFormattedValues() throws Exception {
        // Arrange: mock services to return known values
        when(bookService.getTotalCount()).thenReturn(1500L);
        when(borrowService.countActiveBorrows()).thenReturn(42L);
        when(userService.countUsers()).thenReturn(356L);

        // Create 2 overdue borrows (borrowed 20 days ago, not returned)
        Borrow overdueBorrow1 = createBorrow(20, null);
        Borrow overdueBorrow2 = createBorrow(20, null);
        // Create 1 non-overdue borrow (borrowed 5 days ago, not returned)
        Borrow recentBorrow = createBorrow(5, null);

        when(borrowService.getActiveBorrows()).thenReturn(List.of(overdueBorrow1, overdueBorrow2, recentBorrow));

        // Act: invoke loadKpiData via reflection
        invokeLoadKpiData();

        // Assert: verify labels have correctly formatted text
        assertEquals("1,500", totalBooksValue.getText());
        assertEquals("42", activeLoansValue.getText());
        assertEquals("2", overdueValue.getText());
        assertEquals("356", registeredMembersValue.getText());
    }

    @Test
    void loadKpiData_zeroValues_displaysZero() throws Exception {
        // Arrange
        when(bookService.getTotalCount()).thenReturn(0L);
        when(borrowService.countActiveBorrows()).thenReturn(0L);
        when(userService.countUsers()).thenReturn(0L);
        when(borrowService.getActiveBorrows()).thenReturn(List.of());

        // Act
        invokeLoadKpiData();

        // Assert
        assertEquals("0", totalBooksValue.getText());
        assertEquals("0", activeLoansValue.getText());
        assertEquals("0", overdueValue.getText());
        assertEquals("0", registeredMembersValue.getText());
    }

    @Test
    void loadKpiData_largeNumbers_formatsWithThousandsSeparators() throws Exception {
        // Arrange
        when(bookService.getTotalCount()).thenReturn(1000000L);
        when(borrowService.countActiveBorrows()).thenReturn(12345L);
        when(userService.countUsers()).thenReturn(99999L);
        when(borrowService.getActiveBorrows()).thenReturn(List.of());

        // Act
        invokeLoadKpiData();

        // Assert
        assertEquals("1,000,000", totalBooksValue.getText());
        assertEquals("12,345", activeLoansValue.getText());
        assertEquals("0", overdueValue.getText());
        assertEquals("99,999", registeredMembersValue.getText());
    }

    @Test
    void loadKpiData_allBorrowsOverdue_countsAll() throws Exception {
        // Arrange
        when(bookService.getTotalCount()).thenReturn(100L);
        when(borrowService.countActiveBorrows()).thenReturn(3L);
        when(userService.countUsers()).thenReturn(50L);

        Borrow overdue1 = createBorrow(15, null);
        Borrow overdue2 = createBorrow(30, null);
        Borrow overdue3 = createBorrow(60, null);

        when(borrowService.getActiveBorrows()).thenReturn(List.of(overdue1, overdue2, overdue3));

        // Act
        invokeLoadKpiData();

        // Assert
        assertEquals("100", totalBooksValue.getText());
        assertEquals("3", activeLoansValue.getText());
        assertEquals("3", overdueValue.getText());
        assertEquals("50", registeredMembersValue.getText());
    }

    @Test
    void loadKpiData_returnedBorrows_notCountedAsOverdue() throws Exception {
        // Arrange
        when(bookService.getTotalCount()).thenReturn(500L);
        when(borrowService.countActiveBorrows()).thenReturn(5L);
        when(userService.countUsers()).thenReturn(200L);

        // A borrow that was 20 days ago but already returned - should NOT be overdue
        Borrow returnedBorrow = createBorrow(20, new Date());

        when(borrowService.getActiveBorrows()).thenReturn(List.of(returnedBorrow));

        // Act
        invokeLoadKpiData();

        // Assert: returned borrow is not counted as overdue
        assertEquals("500", totalBooksValue.getText());
        assertEquals("5", activeLoansValue.getText());
        assertEquals("0", overdueValue.getText());
        assertEquals("200", registeredMembersValue.getText());
    }

    // --- Helper methods ---

    private Borrow createBorrow(int daysAgo, Date returnDate) {
        Borrow borrow = new Borrow();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        borrow.setBorrowDate(cal.getTime());
        borrow.setReturnDate(returnDate);
        return borrow;
    }

    private void invokeLoadKpiData() throws Exception {
        Method loadKpiData = DashboardViewController.class.getDeclaredMethod("loadKpiData");
        loadKpiData.setAccessible(true);
        loadKpiData.invoke(controller);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy");
    }
}
