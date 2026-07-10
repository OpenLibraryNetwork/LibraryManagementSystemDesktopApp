package net.gizmolab.library.librarymanagementsystemdesktop.controller;

import app.angeasla.librarymanagementsystemdesktop.service.IBookService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardViewController KPI loading failure path.
 * Validates: Requirements 1.3, 2.3, 3.3, 4.3, 6.3
 *
 * When any service call throws an exception, all KPI labels should be set to "0".
 */
class DashboardViewControllerKpiFailureTest {

    private DashboardViewController controller;
    private IBookService bookService;
    private IBorrowService borrowService;
    private IUserService userService;

    private Label totalBooksValue;
    private Label activeLoansValue;
    private Label overdueValue;
    private Label registeredMembersValue;

    @BeforeAll
    static void initToolkit() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // already initialized
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new DashboardViewController();

        // Create mock services
        bookService = mock(IBookService.class);
        borrowService = mock(IBorrowService.class);
        userService = mock(IUserService.class);

        // Create real JavaFX Labels
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
    void whenBookServiceThrowsException_allKpiLabelsAreSetToZero() throws Exception {
        // Arrange: bookService.getTotalCount() throws RuntimeException
        when(bookService.getTotalCount()).thenThrow(new RuntimeException("Database connection failed"));

        // Act
        invokeLoadKpiData();

        // Assert: all KPI labels should display "0"
        assertEquals("0", totalBooksValue.getText());
        assertEquals("0", activeLoansValue.getText());
        assertEquals("0", overdueValue.getText());
        assertEquals("0", registeredMembersValue.getText());
    }

    @Test
    void whenBorrowServiceCountThrowsException_allKpiLabelsAreSetToZero() throws Exception {
        // Arrange: bookService succeeds but borrowService.countActiveBorrows() throws
        when(bookService.getTotalCount()).thenReturn(10L);
        when(borrowService.countActiveBorrows()).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        invokeLoadKpiData();

        // Assert: all KPI labels should display "0"
        assertEquals("0", totalBooksValue.getText());
        assertEquals("0", activeLoansValue.getText());
        assertEquals("0", overdueValue.getText());
        assertEquals("0", registeredMembersValue.getText());
    }

    @Test
    void whenUserServiceThrowsException_allKpiLabelsAreSetToZero() throws Exception {
        // Arrange: bookService and borrowService succeed but userService throws
        when(bookService.getTotalCount()).thenReturn(10L);
        when(borrowService.countActiveBorrows()).thenReturn(3L);
        when(borrowService.getActiveBorrows()).thenReturn(java.util.Collections.emptyList());
        when(userService.countUsers()).thenThrow(new RuntimeException("User table locked"));

        // Act
        invokeLoadKpiData();

        // Assert: all KPI labels should display "0"
        assertEquals("0", totalBooksValue.getText());
        assertEquals("0", activeLoansValue.getText());
        assertEquals("0", overdueValue.getText());
        assertEquals("0", registeredMembersValue.getText());
    }

    @Test
    void whenBorrowServiceGetActiveBorrowsThrowsException_allKpiLabelsAreSetToZero() throws Exception {
        // Arrange: getActiveBorrows() throws (used for overdue calculation)
        when(bookService.getTotalCount()).thenReturn(10L);
        when(borrowService.countActiveBorrows()).thenReturn(3L);
        when(borrowService.getActiveBorrows()).thenThrow(new RuntimeException("Query timeout"));

        // Act
        invokeLoadKpiData();

        // Assert: all KPI labels should display "0"
        assertEquals("0", totalBooksValue.getText());
        assertEquals("0", activeLoansValue.getText());
        assertEquals("0", overdueValue.getText());
        assertEquals("0", registeredMembersValue.getText());
    }

    /**
     * Uses reflection to invoke the private loadKpiData() method.
     */
    private void invokeLoadKpiData() throws Exception {
        Method method = DashboardViewController.class.getDeclaredMethod("loadKpiData");
        method.setAccessible(true);
        method.invoke(controller);
    }

    /**
     * Uses reflection to set a field value on the controller.
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Finds a field by name traversing the class hierarchy.
     */
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
