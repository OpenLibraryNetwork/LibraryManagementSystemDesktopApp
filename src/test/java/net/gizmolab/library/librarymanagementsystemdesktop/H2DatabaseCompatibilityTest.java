package net.gizmolab.library.librarymanagementsystemdesktop;

import app.angeasla.librarymanagementsystemdesktop.model.*;
import app.angeasla.librarymanagementsystemdesktop.repository.*;
import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.BorrowRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify JPA entity compatibility with H2 database.
 * This test ensures that all entities can be persisted and retrieved correctly.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
public class H2DatabaseCompatibilityTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PublicationCopyRepository publicationCopyRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @Test
    public void testAuthorEntityCompatibility() {
        // Create and save author
        Author author = new Author();
        author.setFirstname("Νίκος");
        author.setLastname("Καζαντζάκης");

        Author savedAuthor = authorRepository.save(author);
        
        assertNotNull(savedAuthor.getAuthorId());
        assertEquals("Νίκος", savedAuthor.getFirstname());
        assertEquals("Καζαντζάκης", savedAuthor.getLastname());
    }

    @Test
    public void testPublisherEntityCompatibility() {
        // Create and save publisher
        Publisher publisher = new Publisher();
        publisher.setName("Εκδόσεις Κέδρος");

        Publisher savedPublisher = publisherRepository.save(publisher);
        
        assertNotNull(savedPublisher.getPublisherId());
        assertEquals("Εκδόσεις Κέδρος", savedPublisher.getName());
    }

    @Test
    public void testBookEntityCompatibility() {
        // Create dependencies
        Author author = new Author();
        author.setFirstname("Νίκος");
        author.setLastname("Καζαντζάκης");
        author = authorRepository.save(author);

        Publisher publisher = new Publisher();
        publisher.setName("Εκδόσεις Κέδρος");
        publisher = publisherRepository.save(publisher);

        // Create and save book
        Book book = new Book();
        book.setTitle("Ζορμπάς ο Γρεκός");
        book.setAuthor(author);
        book.setPublisher(publisher);
        book.setIsbn("978-960-04-0123-4");
        book.setPages(350L);
        book.setPublicationYear(1946L);

        Book savedBook = bookRepository.save(book);
        
        assertNotNull(savedBook.getBookId());
        assertEquals("Ζορμπάς ο Γρεκός", savedBook.getTitle());
        assertEquals(author.getAuthorId(), savedBook.getAuthor().getAuthorId());
        assertEquals(publisher.getPublisherId(), savedBook.getPublisher().getPublisherId());
    }

    @Test
    public void testUserEntityCompatibility() {
        // Create and save user
        User user = new User();
        user.setFirstname("Γιάννης");
        user.setLastname("Παπαδόπουλος");
        user.setEmail("giannis@example.com");
        user.setPhone("6912345678");

        User savedUser = userRepository.save(user);
        
        assertNotNull(savedUser.getUserId());
        assertEquals("Γιάννης", savedUser.getFirstname());
        assertEquals("Παπαδόπουλος", savedUser.getLastname());
        assertEquals("giannis@example.com", savedUser.getEmail());
        assertEquals("6912345678", savedUser.getPhone());
    }

    @Test
    public void testPublicationCopyEntityCompatibility() {
        // Create dependencies
        Author author = new Author();
        author.setFirstname("Νίκος");
        author.setLastname("Καζαντζάκης");
        author = authorRepository.save(author);

        Publisher publisher = new Publisher();
        publisher.setName("Εκδόσεις Κέδρος");
        publisher = publisherRepository.save(publisher);

        Book book = new Book();
        book.setTitle("Ζορμπάς ο Γρεκός");
        book.setAuthor(author);
        book.setPublisher(publisher);
        book = bookRepository.save(book);

        // Create and save book copy
        PublicationCopy bookCopy = new PublicationCopy();
        bookCopy.setBook(book);
        bookCopy.setIsAvailable(true);

        PublicationCopy savedPublicationCopy = publicationCopyRepository.save(bookCopy);
        
        assertNotNull(savedPublicationCopy.getCopyId());
        assertEquals(book.getBookId(), savedPublicationCopy.getBook().getBookId());
        assertTrue(savedPublicationCopy.getIsAvailable());
    }

    @Test
    public void testBorrowEntityCompatibility() {
        // Create all dependencies
        Author author = new Author();
        author.setFirstname("Νίκος");
        author.setLastname("Καζαντζάκης");
        author = authorRepository.save(author);

        Publisher publisher = new Publisher();
        publisher.setName("Εκδόσεις Κέδρος");
        publisher = publisherRepository.save(publisher);

        Book book = new Book();
        book.setTitle("Ζορμπάς ο Γρεκός");
        book.setAuthor(author);
        book.setPublisher(publisher);
        book = bookRepository.save(book);

        PublicationCopy bookCopy = new PublicationCopy();
        bookCopy.setBook(book);
        bookCopy.setIsAvailable(true);
        bookCopy = publicationCopyRepository.save(bookCopy);

        User user = new User();
        user.setFirstname("Γιάννης");
        user.setLastname("Παπαδόπουλος");
        user.setEmail("giannis@example.com");
        user.setPhone("6912345678");
        user = userRepository.save(user);

        // Create and save borrow
        BorrowId borrowId = new BorrowId();
        borrowId.setUserId(user.getUserId());
        borrowId.setPublicationCopyId(bookCopy.getCopyId());
        borrowId.setBorrowTimestamp(java.time.LocalDateTime.now());

        Borrow borrow = new Borrow();
        borrow.setId(borrowId);
        borrow.setUser(user);
        borrow.setPublicationCopy(bookCopy);
        borrow.setBorrowDate(new Date());
        borrow.setReturned(0);

        Borrow savedBorrow = borrowRepository.save(borrow);
        
        assertNotNull(savedBorrow.getId());
        assertEquals(user.getUserId(), savedBorrow.getId().getUserId());
        assertEquals(bookCopy.getCopyId(), savedBorrow.getId().getPublicationCopyId());
        assertNotNull(savedBorrow.getBorrowDate());
        assertEquals(0, savedBorrow.getReturned());
    }

    @Test
    public void testComplexQueryCompatibility() {
        // This test verifies that complex queries work with H2
        // Create test data first
        testBorrowEntityCompatibility();
        
        // Test repository methods that use complex queries
        var allBooks = bookRepository.findAll();
        assertFalse(allBooks.isEmpty());
        
        var allUsers = userRepository.findAll();
        assertFalse(allUsers.isEmpty());
        
        var allBorrows = borrowRepository.findAll();
        assertFalse(allBorrows.isEmpty());
    }
}