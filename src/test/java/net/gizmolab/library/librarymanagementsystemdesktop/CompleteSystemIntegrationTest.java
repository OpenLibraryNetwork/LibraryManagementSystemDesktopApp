package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.config.FXMLLoaderFactory;
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
 * Comprehensive integration test for the entire JavaFX application.
 * Tests all modules working together, data consistency, internationalization, and error handling.
 * 
 * Task 13 Requirements:
 * - Test navigation between all modules (via FXML loading)
 * - Verify data consistency across all CRUD operations
 * - Test internationalization across all modules
 * - Validate error handling in all scenarios
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "javafx.mode=true"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompleteSystemIntegrationTest {

    @Autowired
    private FXMLLoaderFactory fxmlLoaderFactory;

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
    private I18nManager i18nManager;

    @Autowired
    private AlertManager alertManager;

    @Autowired
    private ValidationManager validationManager;

    @BeforeEach
    void setUp() {
        System.setProperty("javafx.mode", "true");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Navigation Between All Modules - FXML Loading Integration")
    void testNavigationBetweenAllModules() {
        // Test that all FXML files can be loaded through the factory
        // This verifies navigation between modules is possible
        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/main-navigation.fxml");
        }, "Main navigation FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/book-management.fxml");
        }, "Book management FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/user-management.fxml");
        }, "User management FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/borrow-management.fxml");
        }, "Borrow management FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/author-management.fxml");
        }, "Author management FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/publisher-management.fxml");
        }, "Publisher management FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/brochure-management.fxml");
        }, "Brochure management FXML should be loadable");

        // Test dialog forms
        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/book-form-dialog.fxml");
        }, "Book form dialog FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/user-form-dialog.fxml");
        }, "User form dialog FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/borrow-form-dialog.fxml");
        }, "Borrow form dialog FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/author-form-dialog.fxml");
        }, "Author form dialog FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/publisher-form-dialog.fxml");
        }, "Publisher form dialog FXML should be loadable");

        assertDoesNotThrow(() -> {
            fxmlLoaderFactory.createLoader("/fxml/brochure-form-dialog.fxml");
        }, "Brochure form dialog FXML should be loadable");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Data Consistency Across All CRUD Operations")
    @Transactional
    void testDataConsistencyAcrossCRUDOperations() {
        // Test Author CRUD
        Author author = new Author();
        author.setFirstname("Test");
        author.setLastname("Author");
        
        Author savedAuthor = authorService.createAuthor(author);
        assertNotNull(savedAuthor.getAuthorId(), "Author should be saved with ID");

        // Test Publisher CRUD
        Publisher publisher = new Publisher();
        publisher.setName("Test Publisher");
        
        Publisher savedPublisher = publisherService.createPublisher(publisher);
        assertNotNull(savedPublisher.getPublisherId(), "Publisher should be saved with ID");

        // Test Book CRUD with relationships
        Book book = new Book();
        book.setTitle("Test Book");
        book.setIsbn("978-0123456789");
        book.setPublicationYear(2023L);
        book.setAuthor(savedAuthor);
        book.setPublisher(savedPublisher);
        
        Book savedBook = bookService.save(book);
        assertNotNull(savedBook.getBookId(), "Book should be saved with ID");
        assertEquals(savedAuthor.getAuthorId(), savedBook.getAuthor().getAuthorId(), "Book should maintain author relationship");
        assertEquals(savedPublisher.getPublisherId(), savedBook.getPublisher().getPublisherId(), "Book should maintain publisher relationship");

        // Test User CRUD
        User user = new User();
        user.setFirstname("Test");
        user.setLastname("User");
        user.setEmail("test@user.com");
        user.setPhone("1234567890");
        
        User savedUser = userService.createUser(user);
        assertNotNull(savedUser.getUserId(), "User should be saved with ID");

        // Test cascading updates
        try {
            savedAuthor.setFirstname("Updated Author");
            Author updatedAuthor = authorService.updateAuthor(savedAuthor.getAuthorId(), savedAuthor);
            assertEquals("Updated Author", updatedAuthor.getFirstname(), "Author update should persist");

            // Verify book still references updated author
            Book bookWithUpdatedAuthor = bookService.getBookByIdToDelete(savedBook.getBookId());
            assertEquals(updatedAuthor.getAuthorId(), bookWithUpdatedAuthor.getAuthor().getAuthorId(), "Book should maintain updated author reference");
        } catch (EntityNotFoundException e) {
            fail("Should not throw EntityNotFoundException for valid entities: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Internationalization Across All Modules")
    void testInternationalizationAcrossModules() {
        // Test English locale
        i18nManager.setLocale(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, i18nManager.getCurrentLocale(), "Should switch to English");

        // Test key message retrieval in English
        String englishSave = i18nManager.getMessage("common.save");
        assertNotNull(englishSave, "Should retrieve English save message");
        assertFalse(englishSave.trim().isEmpty(), "English message should not be empty");

        String englishBooks = i18nManager.getMessage("navigation.books");
        assertNotNull(englishBooks, "Should retrieve English books navigation message");

        String englishUsers = i18nManager.getMessage("navigation.users");
        assertNotNull(englishUsers, "Should retrieve English users navigation message");

        String englishBorrows = i18nManager.getMessage("navigation.borrows");
        assertNotNull(englishBorrows, "Should retrieve English borrows navigation message");

        // Test Greek locale
        i18nManager.setLocale(new Locale("el"));
        assertEquals(new Locale("el"), i18nManager.getCurrentLocale(), "Should switch to Greek");

        // Test key message retrieval in Greek
        String greekSave = i18nManager.getMessage("common.save");
        assertNotNull(greekSave, "Should retrieve Greek save message");
        assertFalse(greekSave.trim().isEmpty(), "Greek message should not be empty");
        assertNotEquals(englishSave, greekSave, "Greek and English messages should be different");

        String greekBooks = i18nManager.getMessage("navigation.books");
        assertNotNull(greekBooks, "Should retrieve Greek books navigation message");
        assertNotEquals(englishBooks, greekBooks, "Greek and English books messages should be different");

        // Test module-specific messages
        String bookTitle = i18nManager.getMessage("book.title");
        assertNotNull(bookTitle, "Should retrieve book title message");

        String userFirstName = i18nManager.getMessage("user.firstName");
        assertNotNull(userFirstName, "Should retrieve user first name message");

        String borrowDate = i18nManager.getMessage("borrow.borrowDate");
        assertNotNull(borrowDate, "Should retrieve borrow date message");

        String authorName = i18nManager.getMessage("author.name");
        assertNotNull(authorName, "Should retrieve author name message");

        String publisherName = i18nManager.getMessage("publisher.name");
        assertNotNull(publisherName, "Should retrieve publisher name message");

        // Test error messages in current locale
        String errorRequired = i18nManager.getMessage("validation.required");
        assertNotNull(errorRequired, "Should retrieve required validation message");

        String errorNotFound = i18nManager.getMessage("error.notFound");
        assertNotNull(errorNotFound, "Should retrieve not found error message");

        // Test locale switching functionality
        assertTrue(i18nManager.isGreek(), "Should detect Greek locale");
        assertFalse(i18nManager.isEnglish(), "Should not detect English when in Greek");

        i18nManager.setLocale(Locale.ENGLISH);
        assertTrue(i18nManager.isEnglish(), "Should detect English locale");
        assertFalse(i18nManager.isGreek(), "Should not detect Greek when in English");

        // Test available locales
        Locale[] availableLocales = i18nManager.getAvailableLocales();
        assertNotNull(availableLocales, "Should have available locales");
        assertTrue(availableLocales.length >= 2, "Should have at least 2 locales");

        // Test display names
        String englishDisplayName = i18nManager.getDisplayName(Locale.ENGLISH);
        assertNotNull(englishDisplayName, "Should have English display name");
        
        String greekDisplayName = i18nManager.getDisplayName(new Locale("el"));
        assertNotNull(greekDisplayName, "Should have Greek display name");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Error Handling Validation Across All Scenarios")
    @Transactional
    void testErrorHandlingAcrossAllScenarios() {
        // Test entity not found scenarios
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

        // Test business logic errors
        // Try to borrow non-existent book
        Optional<Borrow> invalidBorrowResult = borrowService.borrowBook(99999L, 99999L);
        assertFalse(invalidBorrowResult.isPresent(), "Should not create borrow for non-existent entities");

        // Test alert manager availability (UI methods require JavaFX toolkit)
        assertNotNull(alertManager, "Alert manager should be available");

        // Test validation manager functionality
        assertNotNull(validationManager, "Validation manager should be available");
        
        // Test validation result creation
        ValidationManager.ValidationResult validResult = new ValidationManager.ValidationResult(true, null);
        assertTrue(validResult.isValid(), "Valid result should be valid");
        assertTrue(validResult.getErrors().isEmpty(), "Valid result should have no errors");

        ValidationManager.ValidationResult invalidResult = new ValidationManager.ValidationResult(false, List.of("Test error"));
        assertFalse(invalidResult.isValid(), "Invalid result should not be valid");
        assertEquals(1, invalidResult.getErrors().size(), "Invalid result should have errors");
        assertEquals("Test error", invalidResult.getFirstError(), "Should return first error");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: End-to-End Workflow Integration")
    @Transactional
    void testEndToEndWorkflowIntegration() {
        // Create a complete workflow: Author -> Publisher -> Book -> User
        
        // Step 1: Create Author
        Author author = new Author();
        author.setFirstname("Integration");
        author.setLastname("Test Author");
        Author savedAuthor = authorService.createAuthor(author);

        // Step 2: Create Publisher
        Publisher publisher = new Publisher();
        publisher.setName("Integration Test Publisher");
        Publisher savedPublisher = publisherService.createPublisher(publisher);

        // Step 3: Create Book with relationships
        Book book = new Book();
        book.setTitle("Integration Test Book");
        book.setIsbn("978-0987654321");
        book.setPublicationYear(2024L);
        book.setAuthor(savedAuthor);
        book.setPublisher(savedPublisher);
        Book savedBook = bookService.save(book);

        // Step 4: Create User
        User user = new User();
        user.setFirstname("Integration");
        user.setLastname("Test User");
        user.setEmail("integration@testuser.com");
        user.setPhone("9876543210");
        User savedUser = userService.createUser(user);

        // Step 5: Verify all entities still exist and are consistent
        try {
            Author finalAuthor = authorService.getAuthorById(savedAuthor.getAuthorId());
            assertEquals("Integration", finalAuthor.getFirstname(), "Author should remain unchanged");

            Publisher finalPublisher = publisherService.getPublisherById(savedPublisher.getPublisherId());
            assertEquals("Integration Test Publisher", finalPublisher.getName(), "Publisher should remain unchanged");

            Book finalBook = bookService.getBookByIdToDelete(savedBook.getBookId());
            assertEquals(savedAuthor.getAuthorId(), finalBook.getAuthor().getAuthorId(), "Book-Author relationship should remain");
            assertEquals(savedPublisher.getPublisherId(), finalBook.getPublisher().getPublisherId(), "Book-Publisher relationship should remain");

            User finalUser = userService.getUserById(savedUser.getUserId());
            assertEquals("integration@testuser.com", finalUser.getEmail(), "User should remain unchanged");
        } catch (EntityNotFoundException e) {
            fail("Should not throw EntityNotFoundException for valid entities: " + e.getMessage());
        }

        // Verify we can list all entities
        List<Author> authors = authorService.getAllAuthors();
        assertTrue(authors.stream().anyMatch(a -> a.getAuthorId().equals(savedAuthor.getAuthorId())), "Author should be in list");

        List<Publisher> publishers = publisherService.getAllPublishers();
        assertTrue(publishers.stream().anyMatch(p -> p.getPublisherId().equals(savedPublisher.getPublisherId())), "Publisher should be in list");

        List<BookDTO> books = bookService.getAllBooks();
        assertTrue(books.stream().anyMatch(b -> b.getBookId().equals(savedBook.getBookId())), "Book should be in list");

        List<User> users = userService.getAllUsers();
        assertTrue(users.stream().anyMatch(u -> u.getUserId().equals(savedUser.getUserId())), "User should be in list");
    }

    @Test
    @Order(6)
    @DisplayName("Test 6: Service Integration and Cross-Module Dependencies")
    void testServiceIntegrationAndCrossModuleDependencies() {
        // Test that all services are properly injected and working
        assertNotNull(bookService, "Book service should be injected");
        assertNotNull(userService, "User service should be injected");
        assertNotNull(borrowService, "Borrow service should be injected");
        assertNotNull(authorService, "Author service should be injected");
        assertNotNull(publisherService, "Publisher service should be injected");
        assertNotNull(i18nManager, "I18n manager should be injected");
        assertNotNull(alertManager, "Alert manager should be injected");
        assertNotNull(validationManager, "Validation manager should be injected");
        assertNotNull(fxmlLoaderFactory, "FXML loader factory should be injected");

        // Test that services can interact with each other
        List<BookDTO> allBooks = bookService.getAllBooks();
        assertNotNull(allBooks, "Should be able to retrieve all books");

        List<User> allUsers = userService.getAllUsers();
        assertNotNull(allUsers, "Should be able to retrieve all users");

        List<Author> allAuthors = authorService.getAllAuthors();
        assertNotNull(allAuthors, "Should be able to retrieve all authors");

        List<Publisher> allPublishers = publisherService.getAllPublishers();
        assertNotNull(allPublishers, "Should be able to retrieve all publishers");

        // Test search functionality across modules
        List<BookDTO> searchResults = bookService.getBooksByTitle("test");
        assertNotNull(searchResults, "Book search should return results");

        // Test count functionality
        Long bookCount = bookService.getTotalCount();
        assertNotNull(bookCount, "Should be able to count books");

        Long userCount = userService.countUsers();
        assertNotNull(userCount, "Should be able to count users");

        Long authorCount = authorService.countAuthors();
        assertNotNull(authorCount, "Should be able to count authors");

        Long publisherCount = publisherService.countPublishers();
        assertNotNull(publisherCount, "Should be able to count publishers");

        // Test borrow-related functionality
        List<BorrowDTO> allBorrows = borrowService.getAllBorrowsAsDTO();
        assertNotNull(allBorrows, "Should be able to retrieve all borrows as DTO");

        List<BorrowDTO> activeBorrows = borrowService.getActiveBorrowsAsDTO();
        assertNotNull(activeBorrows, "Should be able to retrieve active borrows as DTO");

        Long borrowCount = borrowService.countAllBorrows();
        assertNotNull(borrowCount, "Should be able to count all borrows");

        Long activeBorrowCount = borrowService.countActiveBorrows();
        assertNotNull(activeBorrowCount, "Should be able to count active borrows");
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Cross-Module Data Relationships and Integrity")
    @Transactional
    void testCrossModuleDataRelationshipsAndIntegrity() {
        // Create entities with relationships
        Author author = new Author();
        author.setFirstname("Relationship");
        author.setLastname("Test");
        Author savedAuthor = authorService.createAuthor(author);

        Publisher publisher = new Publisher();
        publisher.setName("Relationship Publisher");
        Publisher savedPublisher = publisherService.createPublisher(publisher);

        // Create multiple books by the same author and publisher
        Book book1 = new Book();
        book1.setTitle("Book One");
        book1.setIsbn("978-1111111111");
        book1.setAuthor(savedAuthor);
        book1.setPublisher(savedPublisher);
        Book savedBook1 = bookService.save(book1);

        Book book2 = new Book();
        book2.setTitle("Book Two");
        book2.setIsbn("978-2222222222");
        book2.setAuthor(savedAuthor);
        book2.setPublisher(savedPublisher);
        Book savedBook2 = bookService.save(book2);

        // Test relationship queries
        List<Book> booksByAuthor = bookService.findBooksByAuthorId(savedAuthor.getAuthorId());
        assertEquals(2, booksByAuthor.size(), "Should find 2 books by the author");
        assertTrue(booksByAuthor.stream().anyMatch(b -> b.getTitle().equals("Book One")), "Should contain Book One");
        assertTrue(booksByAuthor.stream().anyMatch(b -> b.getTitle().equals("Book Two")), "Should contain Book Two");

        List<Book> booksByPublisher = bookService.findBooksByPublisherId(savedPublisher.getPublisherId());
        assertEquals(2, booksByPublisher.size(), "Should find 2 books by the publisher");

        // Test data integrity when updating related entities
        Author updatedAuthor = null;
        try {
            savedAuthor.setFirstname("Updated Relationship");
            updatedAuthor = authorService.updateAuthor(savedAuthor.getAuthorId(), savedAuthor);

            // Verify books still reference the updated author correctly
            Book refreshedBook1 = bookService.getBookByIdToDelete(savedBook1.getBookId());
            assertEquals("Updated Relationship", refreshedBook1.getAuthor().getFirstname(), "Book should reference updated author");

            Book refreshedBook2 = bookService.getBookByIdToDelete(savedBook2.getBookId());
            assertEquals("Updated Relationship", refreshedBook2.getAuthor().getFirstname(), "Book should reference updated author");
        } catch (EntityNotFoundException e) {
            fail("Should not throw EntityNotFoundException for valid entities: " + e.getMessage());
        }

        // Test cascade behavior - verify relationships are maintained
        if (updatedAuthor != null) {
            List<Book> updatedBooksByAuthor = bookService.findBooksByAuthorId(updatedAuthor.getAuthorId());
            assertEquals(2, updatedBooksByAuthor.size(), "Should still find 2 books after author update");
        }
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("javafx.mode");
    }
}