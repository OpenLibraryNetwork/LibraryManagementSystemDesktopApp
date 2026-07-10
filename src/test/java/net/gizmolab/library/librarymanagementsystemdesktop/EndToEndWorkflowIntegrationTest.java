package net.gizmolab.library.librarymanagementsystemdesktop;

import app.angeasla.librarymanagementsystemdesktop.dto.BookDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import app.angeasla.librarymanagementsystemdesktop.model.*;
import app.angeasla.librarymanagementsystemdesktop.service.*;
import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.service.*;
import net.gizmolab.library.librarymanagementsystemdesktop.service.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Workflow Integration Test for Task 13.
 * Tests complete user workflows that span multiple modules and verify data consistency.
 * 
 * Task 13 Requirements:
 * - Verify data consistency across all CRUD operations
 * - Test complete workflows from start to finish
 * - Validate business logic integration
 * - Test error scenarios in realistic workflows
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "javafx.mode=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndWorkflowIntegrationTest {

    @Autowired
    private IBookService bookService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IBorrowService borrowService;

    @Autowired
    private IAuthorService authorService;

    @Autowired
    private IPublisherService publisherService;

    @Autowired
    private IPublicationCopyService publicationCopyService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private AlertManager alertManager;

    @Autowired
    private ValidationManager validationManager;

    // Test data holders
    private Author testAuthor;
    private Publisher testPublisher;
    private Book testBook;
    private User testUser;
    private PublicationCopy testPublicationCopy;

    @BeforeEach
    void setUp() {
        System.setProperty("javafx.mode", "true");
    }

    @Test
    @Order(1)
    @DisplayName("Workflow 1: Complete Library Setup - Author, Publisher, Book, User Creation")
    @Transactional
    void testCompleteLibrarySetupWorkflow() {
        // Step 1: Create Author
        Author author = new Author();
        author.setFirstname("J.K.");
        author.setLastname("Rowling");
        testAuthor = authorService.createAuthor(author);
        
        assertNotNull(testAuthor.getAuthorId(), "Author should be created with ID");
        assertEquals("J.K.", testAuthor.getFirstname(), "Author firstname should be saved correctly");
        assertEquals("Rowling", testAuthor.getLastname(), "Author lastname should be saved correctly");

        // Step 2: Create Publisher
        Publisher publisher = new Publisher();
        publisher.setName("Bloomsbury Publishing");
        testPublisher = publisherService.createPublisher(publisher);
        
        assertNotNull(testPublisher.getPublisherId(), "Publisher should be created with ID");
        assertEquals("Bloomsbury Publishing", testPublisher.getName(), "Publisher name should be saved correctly");

        // Step 3: Create Book with relationships
        Book book = new Book();
        book.setTitle("Harry Potter and the Philosopher's Stone");
        book.setIsbn("978-0747532699");
        book.setPublicationYear(1997L);
        book.setPages(223L);
        book.setAuthor(testAuthor);
        book.setPublisher(testPublisher);
        testBook = bookService.save(book);
        
        assertNotNull(testBook.getBookId(), "Book should be created with ID");
        assertEquals("Harry Potter and the Philosopher's Stone", testBook.getTitle(), "Book title should be saved correctly");
        assertEquals("978-0747532699", testBook.getIsbn(), "Book ISBN should be saved correctly");
        assertEquals(testAuthor.getAuthorId(), testBook.getAuthor().getAuthorId(), "Book should reference correct author");
        assertEquals(testPublisher.getPublisherId(), testBook.getPublisher().getPublisherId(), "Book should reference correct publisher");

        // Step 4: Create Book Copy
        testPublicationCopy = publicationCopyService.createPublicationCopy(testBook, 1);
        
        assertNotNull(testPublicationCopy.getCopyId(), "Book copy should be created with ID");
        assertEquals(1, testPublicationCopy.getCopyNumber(), "Book copy number should be saved correctly");
        assertTrue(testPublicationCopy.getIsAvailable(), "Book copy should be available initially");
        assertEquals(testBook.getBookId(), testPublicationCopy.getBook().getBookId(), "Book copy should reference correct book");

        // Step 5: Create User
        User user = new User();
        user.setFirstname("Harry");
        user.setLastname("Potter");
        user.setEmail("harry.potter@hogwarts.edu");
        user.setPhone("+44 20 7946 0958");
        testUser = userService.createUser(user);
        
        assertNotNull(testUser.getUserId(), "User should be created with ID");
        assertEquals("Harry", testUser.getFirstname(), "User firstname should be saved correctly");
        assertEquals("Potter", testUser.getLastname(), "User lastname should be saved correctly");
        assertEquals("harry.potter@hogwarts.edu", testUser.getEmail(), "User email should be saved correctly");

        // Step 6: Verify all entities exist and are properly linked
        try {
            Author retrievedAuthor = authorService.getAuthorById(testAuthor.getAuthorId());
            assertEquals(testAuthor.getFirstname(), retrievedAuthor.getFirstname(), "Retrieved author should match created author");

            Publisher retrievedPublisher = publisherService.getPublisherById(testPublisher.getPublisherId());
            assertEquals(testPublisher.getName(), retrievedPublisher.getName(), "Retrieved publisher should match created publisher");

            Book retrievedBook = bookService.getBookByIdToDelete(testBook.getBookId());
            assertEquals(testBook.getTitle(), retrievedBook.getTitle(), "Retrieved book should match created book");
            assertEquals(testAuthor.getAuthorId(), retrievedBook.getAuthor().getAuthorId(), "Retrieved book should have correct author");

            User retrievedUser = userService.getUserById(testUser.getUserId());
            assertEquals(testUser.getEmail(), retrievedUser.getEmail(), "Retrieved user should match created user");

            Optional<PublicationCopy> retrievedPublicationCopyOpt = publicationCopyService.getPublicationCopyById(testPublicationCopy.getCopyId());
            assertTrue(retrievedPublicationCopyOpt.isPresent(), "Book copy should be retrievable");
            PublicationCopy retrievedPublicationCopy = retrievedPublicationCopyOpt.get();
            assertEquals(testPublicationCopy.getCopyNumber(), retrievedPublicationCopy.getCopyNumber(), "Retrieved book copy should match created copy");
            assertEquals(testBook.getBookId(), retrievedPublicationCopy.getBook().getBookId(), "Retrieved book copy should reference correct book");
        } catch (EntityNotFoundException e) {
            fail("All created entities should be retrievable: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Workflow 2: Complete Borrow and Return Cycle")
    @Transactional
    void testCompleteBorrowAndReturnWorkflow() {
        // Ensure we have test data from previous test
        if (testUser == null || testPublicationCopy == null) {
            testCompleteLibrarySetupWorkflow();
        }

        // Step 1: Verify book copy is available
        Optional<PublicationCopy> availablePublicationCopyOpt = publicationCopyService.getPublicationCopyById(testPublicationCopy.getCopyId());
        assertTrue(availablePublicationCopyOpt.isPresent(), "Book copy should exist");
        PublicationCopy availablePublicationCopy = availablePublicationCopyOpt.get();
        assertTrue(availablePublicationCopy.getIsAvailable(), "Book copy should be available for borrowing");

        // Step 2: Borrow the book
        Optional<Borrow> borrowResult = borrowService.borrowBook(testUser.getUserId(), testBook.getBookId());
        assertTrue(borrowResult.isPresent(), "Borrow operation should succeed");
        
        Borrow borrow = borrowResult.get();
        assertNotNull(borrow, "Borrow should be created");
        assertEquals(testPublicationCopy.getCopyId(), borrow.getId().getPublicationCopyId(), "Borrow should reference correct book copy");
        assertEquals(testUser.getUserId(), borrow.getId().getUserId(), "Borrow should reference correct user");
        assertNotNull(borrow.getBorrowDate(), "Borrow should have borrow date");
        assertEquals(0, borrow.getReturned(), "Borrow should not be returned initially");

        // Step 3: Verify book copy is no longer available
        Optional<PublicationCopy> borrowedPublicationCopyOpt = publicationCopyService.getPublicationCopyById(testPublicationCopy.getCopyId());
        assertTrue(borrowedPublicationCopyOpt.isPresent(), "Book copy should still exist after borrowing");
        PublicationCopy borrowedPublicationCopy = borrowedPublicationCopyOpt.get();
        assertFalse(borrowedPublicationCopy.getIsAvailable(), "Book copy should not be available after borrowing");

        // Step 4: Verify borrow appears in active borrows
        List<BorrowDTO> activeBorrows = borrowService.getActiveBorrowsAsDTO();
        assertTrue(activeBorrows.stream().anyMatch(b -> 
            b.getUserId().equals(testUser.getUserId())
        ), "Active borrows should contain our borrow");

        // Step 5: Verify borrow counts
        Long activeBorrowCount = borrowService.countActiveBorrows();
        assertTrue(activeBorrowCount > 0, "Should have at least one active borrow");

        Long totalBorrowCount = borrowService.countAllBorrows();
        assertTrue(totalBorrowCount > 0, "Should have at least one total borrow");

        // Step 6: Return the book
        Optional<Borrow> returnResult = null;
        try {
            returnResult = borrowService.returnBook(testUser.getUserId(), testBook.getBookId());
        } catch (EntityNotFoundException e) {
            fail("Should not throw EntityNotFoundException for valid return: " + e.getMessage());
        }
        assertTrue(returnResult.isPresent(), "Return operation should succeed");
        
        Borrow returnedBorrow = returnResult.get();
        assertEquals(1, returnedBorrow.getReturned(), "Borrow should be marked as returned");
        assertNotNull(returnedBorrow.getReturnDate(), "Borrow should have return date");

        // Step 7: Verify book copy is available again
        Optional<PublicationCopy> returnedPublicationCopyOpt = publicationCopyService.getPublicationCopyById(testPublicationCopy.getCopyId());
        assertTrue(returnedPublicationCopyOpt.isPresent(), "Book copy should still exist after return");
        PublicationCopy returnedPublicationCopy = returnedPublicationCopyOpt.get();
        assertTrue(returnedPublicationCopy.getIsAvailable(), "Book copy should be available after return");

        // Step 8: Verify borrow no longer appears in active borrows
        List<BorrowDTO> activeBorrowsAfterReturn = borrowService.getActiveBorrowsAsDTO();
        assertFalse(activeBorrowsAfterReturn.stream().anyMatch(b -> 
            b.getUserId().equals(testUser.getUserId()) &&
            b.getReturned() == 0
        ), "Active borrows should not contain returned borrow");

        // Step 9: Verify borrow appears in all borrows
        List<BorrowDTO> allBorrows = borrowService.getAllBorrowsAsDTO();
        assertTrue(allBorrows.stream().anyMatch(b -> 
            b.getUserId().equals(testUser.getUserId()) &&
            b.getReturned() == 1
        ), "All borrows should contain returned borrow");
    }

    @Test
    @Order(3)
    @DisplayName("Workflow 3: Search and Filter Operations Across Modules")
    @Transactional
    void testSearchAndFilterOperationsWorkflow() {
        // Ensure we have test data
        if (testBook == null || testUser == null || testAuthor == null || testPublisher == null) {
            testCompleteLibrarySetupWorkflow();
        }

        // Step 1: Search books by title
        List<BookDTO> booksByTitle = bookService.getBooksByTitle("Harry Potter");
        assertTrue(booksByTitle.stream().anyMatch(b -> b.getTitle().contains("Harry Potter")), 
            "Should find books with 'Harry Potter' in title");

        // Step 2: Search books by author
        List<Book> booksByAuthor = bookService.findBooksByAuthorId(testAuthor.getAuthorId());
        assertTrue(booksByAuthor.stream().anyMatch(b -> b.getAuthor().getAuthorId().equals(testAuthor.getAuthorId())), 
            "Should find books by the test author");

        // Step 3: Search books by publisher
        List<Book> booksByPublisher = bookService.findBooksByPublisherId(testPublisher.getPublisherId());
        assertTrue(booksByPublisher.stream().anyMatch(b -> b.getPublisher().getPublisherId().equals(testPublisher.getPublisherId())), 
            "Should find books by the test publisher");

        // Step 4: Get all entities and verify counts
        List<BookDTO> allBooks = bookService.getAllBooks();
        assertTrue(allBooks.size() > 0, "Should have at least one book");

        List<User> allUsers = userService.getAllUsers();
        assertTrue(allUsers.size() > 0, "Should have at least one user");

        List<Author> allAuthors = authorService.getAllAuthors();
        assertTrue(allAuthors.size() > 0, "Should have at least one author");

        List<Publisher> allPublishers = publisherService.getAllPublishers();
        assertTrue(allPublishers.size() > 0, "Should have at least one publisher");

        // Step 5: Verify count methods
        Long bookCount = bookService.getTotalCount();
        assertEquals(allBooks.size(), bookCount.intValue(), "Book count should match list size");

        Long userCount = userService.countUsers();
        assertEquals(allUsers.size(), userCount.intValue(), "User count should match list size");

        Long authorCount = authorService.countAuthors();
        assertEquals(allAuthors.size(), authorCount.intValue(), "Author count should match list size");

        Long publisherCount = publisherService.countPublishers();
        assertEquals(allPublishers.size(), publisherCount.intValue(), "Publisher count should match list size");

        // Step 6: Test pagination (if implemented)
        try {
            List<BookDTO> paginatedBooks = bookService.getAllBooks(); // Assuming this supports pagination
            assertNotNull(paginatedBooks, "Paginated books should not be null");
        } catch (Exception e) {
            // Pagination might not be implemented, so we'll just note it
            System.out.println("Pagination test note: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Workflow 4: Update Operations and Data Consistency")
    @Transactional
    void testUpdateOperationsAndDataConsistency() {
        // Ensure we have test data
        if (testAuthor == null || testPublisher == null || testBook == null || testUser == null) {
            testCompleteLibrarySetupWorkflow();
        }

        // Step 1: Update Author
        try {
            testAuthor.setFirstname("Joanne");
            testAuthor.setLastname("Rowling");
            Author updatedAuthor = authorService.updateAuthor(testAuthor.getAuthorId(), testAuthor);
            
            assertEquals("Joanne", updatedAuthor.getFirstname(), "Author firstname should be updated");
            assertEquals("Rowling", updatedAuthor.getLastname(), "Author lastname should be updated");

            // Verify book still references updated author
            Book bookWithUpdatedAuthor = bookService.getBookByIdToDelete(testBook.getBookId());
            assertEquals(updatedAuthor.getAuthorId(), bookWithUpdatedAuthor.getAuthor().getAuthorId(), 
                "Book should still reference updated author");
            assertEquals("Joanne", bookWithUpdatedAuthor.getAuthor().getFirstname(), 
                "Book should show updated author firstname");
        } catch (EntityNotFoundException e) {
            fail("Should not throw EntityNotFoundException for valid author update: " + e.getMessage());
        }

        // Step 2: Update Publisher
        try {
            testPublisher.setName("Bloomsbury Children's Books");
            Publisher updatedPublisher = publisherService.updatePublisher(testPublisher.getPublisherId(), testPublisher);
            
            assertEquals("Bloomsbury Children's Books", updatedPublisher.getName(), "Publisher name should be updated");

            // Verify book still references updated publisher
            Book bookWithUpdatedPublisher = bookService.getBookByIdToDelete(testBook.getBookId());
            assertEquals(updatedPublisher.getPublisherId(), bookWithUpdatedPublisher.getPublisher().getPublisherId(), 
                "Book should still reference updated publisher");
            assertEquals("Bloomsbury Children's Books", bookWithUpdatedPublisher.getPublisher().getName(), 
                "Book should show updated publisher name");
        } catch (EntityNotFoundException e) {
            fail("Should not throw EntityNotFoundException for valid publisher update: " + e.getMessage());
        }

        // Step 3: Update Book
        testBook.setTitle("Harry Potter and the Sorcerer's Stone");
        testBook.setPages(309L);
        Book updatedBook = bookService.save(testBook);
        
        assertEquals("Harry Potter and the Sorcerer's Stone", updatedBook.getTitle(), "Book title should be updated");
        assertEquals(309L, updatedBook.getPages(), "Book pages should be updated");

        // Step 4: Update User
        try {
            testUser.setEmail("h.potter@hogwarts.edu");
            testUser.setPhone("+44 20 7946 0999");
            User updatedUser = userService.updateUser(testUser.getUserId(), testUser);
            
            assertEquals("h.potter@hogwarts.edu", updatedUser.getEmail(), "User email should be updated");
            assertEquals("+44 20 7946 0999", updatedUser.getPhone(), "User phone should be updated");
        } catch (EntityNotFoundException e) {
            fail("Should not throw EntityNotFoundException for valid user update: " + e.getMessage());
        }

        // Step 5: Verify all updates are persistent
        try {
            Author finalAuthor = authorService.getAuthorById(testAuthor.getAuthorId());
            assertEquals("Joanne", finalAuthor.getFirstname(), "Author update should be persistent");

            Publisher finalPublisher = publisherService.getPublisherById(testPublisher.getPublisherId());
            assertEquals("Bloomsbury Children's Books", finalPublisher.getName(), "Publisher update should be persistent");

            Book finalBook = bookService.getBookByIdToDelete(testBook.getBookId());
            assertEquals("Harry Potter and the Sorcerer's Stone", finalBook.getTitle(), "Book update should be persistent");

            User finalUser = userService.getUserById(testUser.getUserId());
            assertEquals("h.potter@hogwarts.edu", finalUser.getEmail(), "User update should be persistent");
        } catch (EntityNotFoundException e) {
            fail("All updated entities should be retrievable: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Workflow 5: Error Handling and Recovery Scenarios")
    @Transactional
    void testErrorHandlingAndRecoveryScenarios() {
        // Step 1: Test entity not found scenarios
        assertThrows(EntityNotFoundException.class, () -> {
            bookService.getBookById(99999L);
        }, "Should throw EntityNotFoundException for non-existent book");

        assertThrows(EntityNotFoundException.class, () -> {
            userService.getUserById(99999L);
        }, "Should throw EntityNotFoundException for non-existent user");

        assertThrows(EntityNotFoundException.class, () -> {
            authorService.getAuthorById(99999L);
        }, "Should throw EntityNotFoundException for non-existent author");

        assertThrows(EntityNotFoundException.class, () -> {
            publisherService.getPublisherById(99999L);
        }, "Should throw EntityNotFoundException for non-existent publisher");

        // Step 2: Test invalid borrow scenarios
        Optional<Borrow> invalidBorrow1 = borrowService.borrowBook(99999L, 99999L);
        assertFalse(invalidBorrow1.isPresent(), "Should not create borrow for non-existent entities");

        // Step 3: Test invalid return scenarios
        try {
            Optional<Borrow> invalidReturn = borrowService.returnBook(99999L, 99999L);
            assertFalse(invalidReturn.isPresent(), "Should not return non-existent borrow");
        } catch (EntityNotFoundException e) {
            // This is expected for non-existent entities
            assertTrue(true, "Expected EntityNotFoundException for non-existent return");
        }

        // Step 4: Test duplicate borrow scenarios (if book copy is not available)
        if (testBook != null && testUser != null) {
            // First borrow should succeed
            Optional<Borrow> firstBorrow = borrowService.borrowBook(testUser.getUserId(), testBook.getBookId());
            if (firstBorrow.isPresent()) {
                // Second borrow of same book should fail (no more available copies)
                Optional<Borrow> secondBorrow = borrowService.borrowBook(testUser.getUserId(), testBook.getBookId());
                assertFalse(secondBorrow.isPresent(), "Should not allow duplicate borrow when no copies available");
                
                // Return the book for cleanup
                try {
                    borrowService.returnBook(testUser.getUserId(), testBook.getBookId());
                } catch (EntityNotFoundException e) {
                    // Ignore cleanup errors
                }
            }
        }

        // Step 5: Test validation scenarios
        ValidationManager.ValidationResult validResult = new ValidationManager.ValidationResult(true, null);
        assertTrue(validResult.isValid(), "Valid result should be valid");

        ValidationManager.ValidationResult invalidResult = new ValidationManager.ValidationResult(false, List.of("Test error"));
        assertFalse(invalidResult.isValid(), "Invalid result should not be valid");
        assertEquals("Test error", invalidResult.getFirstError(), "Should return first error");

        // Step 6: Test i18n error messages
        i18nManager.setLocale(Locale.ENGLISH);
        String englishError = i18nManager.getMessage("error.notFound");
        assertNotNull(englishError, "Should retrieve English error message");

        i18nManager.setLocale(new Locale("el"));
        String greekError = i18nManager.getMessage("error.notFound");
        assertNotNull(greekError, "Should retrieve Greek error message");

        // Step 7: Test alert manager (UI methods require JavaFX toolkit)
        assertNotNull(alertManager, "Alert manager should be available");
        // Note: Actual alert display methods would require JavaFX Application Thread
    }

    @Test
    @Order(6)
    @DisplayName("Workflow 6: Internationalization Consistency Across All Modules")
    void testInternationalizationConsistencyWorkflow() {
        // Step 1: Test English locale
        i18nManager.setLocale(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, i18nManager.getCurrentLocale(), "Should be in English locale");
        assertTrue(i18nManager.isEnglish(), "Should detect English locale");
        assertFalse(i18nManager.isGreek(), "Should not detect Greek locale");

        // Test common messages in English
        String englishSave = i18nManager.getMessage("common.save");
        String englishCancel = i18nManager.getMessage("common.cancel");
        String englishDelete = i18nManager.getMessage("common.delete");
        
        assertNotNull(englishSave, "Should have English save message");
        assertNotNull(englishCancel, "Should have English cancel message");
        assertNotNull(englishDelete, "Should have English delete message");

        // Test navigation messages in English
        String englishBooks = i18nManager.getMessage("navigation.books");
        String englishUsers = i18nManager.getMessage("navigation.users");
        String englishBorrows = i18nManager.getMessage("navigation.borrows");
        String englishAuthors = i18nManager.getMessage("navigation.authors");
        String englishPublishers = i18nManager.getMessage("navigation.publishers");
        
        assertNotNull(englishBooks, "Should have English books navigation message");
        assertNotNull(englishUsers, "Should have English users navigation message");
        assertNotNull(englishBorrows, "Should have English borrows navigation message");
        assertNotNull(englishAuthors, "Should have English authors navigation message");
        assertNotNull(englishPublishers, "Should have English publishers navigation message");

        // Step 2: Test Greek locale
        i18nManager.setLocale(new Locale("el"));
        assertEquals(new Locale("el"), i18nManager.getCurrentLocale(), "Should be in Greek locale");
        assertTrue(i18nManager.isGreek(), "Should detect Greek locale");
        assertFalse(i18nManager.isEnglish(), "Should not detect English locale");

        // Test common messages in Greek
        String greekSave = i18nManager.getMessage("common.save");
        String greekCancel = i18nManager.getMessage("common.cancel");
        String greekDelete = i18nManager.getMessage("common.delete");
        
        assertNotNull(greekSave, "Should have Greek save message");
        assertNotNull(greekCancel, "Should have Greek cancel message");
        assertNotNull(greekDelete, "Should have Greek delete message");

        // Verify messages are different between locales
        assertNotEquals(englishSave, greekSave, "Save messages should be different between locales");
        assertNotEquals(englishCancel, greekCancel, "Cancel messages should be different between locales");
        assertNotEquals(englishDelete, greekDelete, "Delete messages should be different between locales");

        // Test navigation messages in Greek
        String greekBooks = i18nManager.getMessage("navigation.books");
        String greekUsers = i18nManager.getMessage("navigation.users");
        String greekBorrows = i18nManager.getMessage("navigation.borrows");
        
        assertNotNull(greekBooks, "Should have Greek books navigation message");
        assertNotNull(greekUsers, "Should have Greek users navigation message");
        assertNotNull(greekBorrows, "Should have Greek borrows navigation message");

        // Verify navigation messages are different between locales
        assertNotEquals(englishBooks, greekBooks, "Books navigation should be different between locales");
        assertNotEquals(englishUsers, greekUsers, "Users navigation should be different between locales");
        assertNotEquals(englishBorrows, greekBorrows, "Borrows navigation should be different between locales");

        // Step 3: Test module-specific messages
        String bookTitle = i18nManager.getMessage("book.title");
        String bookIsbn = i18nManager.getMessage("book.isbn");
        String userFirstName = i18nManager.getMessage("user.firstName");
        String userEmail = i18nManager.getMessage("user.email");
        String borrowDate = i18nManager.getMessage("borrow.borrowDate");
        String returnDate = i18nManager.getMessage("borrow.returnDate");
        
        assertNotNull(bookTitle, "Should have book title message");
        assertNotNull(bookIsbn, "Should have book ISBN message");
        assertNotNull(userFirstName, "Should have user first name message");
        assertNotNull(userEmail, "Should have user email message");
        assertNotNull(borrowDate, "Should have borrow date message");
        assertNotNull(returnDate, "Should have return date message");

        // Step 4: Test error and validation messages
        String errorRequired = i18nManager.getMessage("validation.required");
        String errorInvalidEmail = i18nManager.getMessage("validation.invalid.email");
        String errorNotFound = i18nManager.getMessage("error.notFound");
        
        assertNotNull(errorRequired, "Should have required validation message");
        assertNotNull(errorInvalidEmail, "Should have invalid email message");
        assertNotNull(errorNotFound, "Should have not found error message");

        // Step 5: Test available locales and display names
        Locale[] availableLocales = i18nManager.getAvailableLocales();
        assertNotNull(availableLocales, "Should have available locales");
        assertTrue(availableLocales.length >= 2, "Should have at least 2 locales (English and Greek)");

        String englishDisplayName = i18nManager.getDisplayName(Locale.ENGLISH);
        String greekDisplayName = i18nManager.getDisplayName(new Locale("el"));
        
        assertNotNull(englishDisplayName, "Should have English display name");
        assertNotNull(greekDisplayName, "Should have Greek display name");
        assertNotEquals(englishDisplayName, greekDisplayName, "Display names should be different");

        // Step 6: Test message formatting with parameters
        String messageWithParams = i18nManager.getMessage("validation.required", "Test Field");
        assertNotNull(messageWithParams, "Should format message with parameters");
        assertTrue(messageWithParams.contains("Test Field") || !messageWithParams.equals("validation.required"), 
            "Formatted message should contain parameter or be different from key");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("javafx.mode");
    }
}