package net.gizmolab.library.librarymanagementsystemdesktop;

import app.angeasla.librarymanagementsystemdesktop.model.Author;
import app.angeasla.librarymanagementsystemdesktop.model.Book;
import app.angeasla.librarymanagementsystemdesktop.model.PublicationCopy;
import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import app.angeasla.librarymanagementsystemdesktop.model.Publisher;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import app.angeasla.librarymanagementsystemdesktop.repository.AuthorRepository;
import app.angeasla.librarymanagementsystemdesktop.repository.PublicationCopyRepository;
import app.angeasla.librarymanagementsystemdesktop.repository.BookRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.BorrowRepository;
import app.angeasla.librarymanagementsystemdesktop.repository.PublisherRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.UserRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.service.BorrowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test και Preservation Tests για το σφάλμα δανεισμού πολλαπλών αντιτύπων
 * 
 * Task 1 - Bug Condition Exploration:
 * ΣΗΜΑΝΤΙΚΟ: Το test διερεύνησης ΠΡΕΠΕΙ ΝΑ ΑΠΟΤΥΧΕΙ στον μη διορθωμένο κώδικα.
 * Η αποτυχία επιβεβαιώνει ότι το σφάλμα υπάρχει.
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4**
 * 
 * Task 2 - Preservation Tests:
 * Τα preservation tests ΠΡΕΠΕΙ ΝΑ ΠΕΡΝΟΥΝ στον μη διορθωμένο κώδικα.
 * Αυτό επιβεβαιώνει τη baseline συμπεριφορά που πρέπει να διατηρηθεί.
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 */
@SpringBootTest
@Transactional
public class BorrowSingleCopyBugfixTest {

    @Autowired
    private BorrowServiceImpl borrowService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PublicationCopyRepository publicationCopyRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    private Random random = new Random();
    private List<Long> createdUserIds = new ArrayList<>();
    private List<Long> createdBookIds = new ArrayList<>();
    private List<Long> createdCopyIds = new ArrayList<>();
    private List<Long> createdAuthorIds = new ArrayList<>();
    private List<Long> createdPublisherIds = new ArrayList<>();

    /**
     * Property 1: Fault Condition - Δανεισμός Συγκεκριμένου Αντιτύπου
     * 
     * Αυτό το property test επαληθεύει ότι όταν ο χρήστης επιλέγει ένα συγκεκριμένο
     * αντίτυπο για δανεισμό, το σύστημα δανείζει ΜΟΝΟ αυτό το αντίτυπο και όχι άλλα.
     * 
     * ΑΝΑΜΕΝΟΜΕΝΟ: Αυτό το test θα ΑΠΟΤΥΧΕΙ στον μη διορθωμένο κώδικα επειδή:
     * - Η μέθοδος borrowBook() δέχεται μόνο (userId, bookId) χωρίς bookCopyId
     * - Το σύστημα επιλέγει αυτόματα το πρώτο διαθέσιμο αντίτυπο
     * - Μπορεί να δημιουργηθούν πολλαπλές εγγραφές δανεισμού
     */
    @RepeatedTest(10)
    void whenUserSelectsSpecificCopy_thenOnlyThatCopyIsBorrowed() {
        // Generate random test parameters
        int totalCopies = 2 + random.nextInt(4); // 2-5 copies
        int selectedCopyIndex = random.nextInt(totalCopies); // 0 to totalCopies-1
        
        // Setup: Create test data
        User user = createTestUser();
        Book book = createTestBook();
        List<PublicationCopy> copies = createMultipleCopies(book, totalCopies);
        
        // The copy that the user selects (simulating UI selection)
        PublicationCopy selectedCopy = copies.get(selectedCopyIndex);
        Long selectedCopyId = selectedCopy.getCopyId();
        
        // Action: Simulate borrowing through UI with the specific bookCopyId
        // Using the new overloaded method that accepts bookCopyId directly
        Optional<Borrow> result = borrowService.borrowBook(user.getUserId(), selectedCopyId, true);
        
        // Verification: Check expected behavior
        
        // 1. A borrow record should be created
        assertTrue(result.isPresent(), 
            "Το σύστημα πρέπει να δημιουργεί εγγραφή δανεισμού");
        
        // 2. Only ONE borrow record should be created
        List<Borrow> allBorrows = borrowRepository.findByUserAndReturned(user, 0);
        assertEquals(1, allBorrows.size(), 
            String.format("Πρέπει να δημιουργηθεί μόνο 1 εγγραφή δανεισμού, αλλά βρέθηκαν %d", 
                allBorrows.size()));
        
        // 3. The borrowed copy should be the one the user selected
        Borrow borrow = result.get();
        assertEquals(selectedCopyId, borrow.getPublicationCopy().getCopyId(),
            String.format("Το σύστημα πρέπει να δανείσει το Copy #%d (copyId=%d) που επέλεξε ο χρήστης, " +
                "αλλά δάνεισε το Copy #%d (copyId=%d)",
                selectedCopy.getCopyNumber(), selectedCopyId,
                borrow.getPublicationCopy().getCopyNumber(), borrow.getPublicationCopy().getCopyId()));
        
        // 4. Only the selected copy should be marked as unavailable
        PublicationCopy borrowedCopy = publicationCopyRepository.findById(selectedCopyId).orElseThrow();
        assertFalse(borrowedCopy.getIsAvailable(),
            String.format("Το επιλεγμένο αντίτυπο (Copy #%d) πρέπει να είναι isAvailable=false",
                selectedCopy.getCopyNumber()));
        
        // 5. All other copies should remain available
        for (PublicationCopy copy : copies) {
            if (!copy.getCopyId().equals(selectedCopyId)) {
                PublicationCopy otherCopy = publicationCopyRepository.findById(copy.getCopyId()).orElseThrow();
                assertTrue(otherCopy.getIsAvailable(),
                    String.format("Το αντίτυπο Copy #%d (copyId=%d) πρέπει να παραμείνει isAvailable=true, " +
                        "αλλά είναι isAvailable=false",
                        otherCopy.getCopyNumber(), otherCopy.getCopyId()));
            }
        }
    }

    // Helper methods
    
    private User createTestUser() {
        User user = new User();
        user.setFirstname("Test");
        user.setLastname("User");
        user.setEmail("test" + System.nanoTime() + "@example.com");
        user.setPhone("" + System.nanoTime()); // Unique phone
        user = userRepository.save(user);
        createdUserIds.add(user.getUserId());
        return user;
    }

    private Book createTestBook() {
        // Create author
        Author author = new Author();
        author.setFirstname("Test");
        author.setLastname("Author" + System.nanoTime());
        author = authorRepository.save(author);
        createdAuthorIds.add(author.getAuthorId());
        
        // Create publisher
        Publisher publisher = new Publisher();
        publisher.setName("Test Publisher " + System.nanoTime());
        publisher = publisherRepository.save(publisher);
        createdPublisherIds.add(publisher.getPublisherId());
        
        // Create book
        Book book = new Book();
        book.setTitle("Test Book " + System.nanoTime());
        book.setIsbn("ISBN-" + System.nanoTime());
        book.setAuthor(author);
        book.setPublisher(publisher);
        book.setPages(200L);
        book.setPublicationYear(2023L);
        book = bookRepository.save(book);
        createdBookIds.add(book.getBookId());
        return book;
    }

    private List<PublicationCopy> createMultipleCopies(Book book, int count) {
        List<PublicationCopy> copies = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            PublicationCopy copy = new PublicationCopy();
            copy.setBook(book);
            copy.setCopyNumber(i);
            copy.setIsAvailable(true);
            copy.setCondition("GOOD");
            copy = publicationCopyRepository.save(copy);
            createdCopyIds.add(copy.getCopyId());
            copies.add(copy);
        }
        return copies;
    }

    /**
     * Property 2: Preservation - Test 1: Επιστροφή Βιβλίων
     * 
     * Επαληθεύει ότι η returnBook() συνεχίζει να λειτουργεί σωστά.
     * Όταν ένας χρήστης επιστρέφει ένα δανεισμένο βιβλίο, το σύστημα πρέπει να:
     * - Μαρκάρει την εγγραφή ως επιστραμμένη (returned=1)
     * - Ενημερώνει το αντίτυπο ως διαθέσιμο (isAvailable=true)
     * 
     * **Validates: Requirements 3.1**
     */
    @RepeatedTest(10)
    void preservation_returnBook_worksCorrectly() throws Exception {
        // Setup: Create user, book, and borrow a copy
        User user = createTestUser();
        Book book = createTestBook();
        List<PublicationCopy> copies = createMultipleCopies(book, 2 + random.nextInt(3)); // 2-4 copies
        
        // Borrow a book
        Optional<Borrow> borrowResult = borrowService.borrowBook(user.getUserId(), book.getBookId());
        assertTrue(borrowResult.isPresent(), "Ο δανεισμός πρέπει να είναι επιτυχής");
        
        Borrow borrow = borrowResult.get();
        Long borrowedCopyId = borrow.getPublicationCopy().getCopyId();
        
        // Action: Return the book
        Optional<Borrow> returnResult = borrowService.returnBook(user.getUserId(), book.getBookId());
        
        // Verification: Check that return works correctly
        assertTrue(returnResult.isPresent(), "Η επιστροφή πρέπει να είναι επιτυχής");
        
        Borrow returnedBorrow = returnResult.get();
        assertEquals(1, returnedBorrow.getReturned(), 
            "Η εγγραφή δανεισμού πρέπει να μαρκαριστεί ως επιστραμμένη (returned=1)");
        assertNotNull(returnedBorrow.getReturnDate(), 
            "Η ημερομηνία επιστροφής πρέπει να οριστεί");
        
        // Verify the copy is available again
        PublicationCopy returnedCopy = publicationCopyRepository.findById(borrowedCopyId).orElseThrow();
        assertTrue(returnedCopy.getIsAvailable(), 
            "Το αντίτυπο πρέπει να είναι διαθέσιμο μετά την επιστροφή (isAvailable=true)");
    }

    /**
     * Property 2: Preservation - Test 2: Εμφάνιση Διαθέσιμων Αντιτύπων
     * 
     * Επαληθεύει ότι η λίστα διαθέσιμων αντιτύπων εμφανίζεται σωστά.
     * Όταν ένα βιβλίο έχει πολλαπλά αντίτυπα, το σύστημα πρέπει να εμφανίζει
     * μόνο τα διαθέσιμα αντίτυπα (isAvailable=true).
     * 
     * **Validates: Requirements 3.2**
     */
    @RepeatedTest(10)
    void preservation_availableCopiesDisplay_worksCorrectly() {
        // Setup: Create book with multiple copies
        Book book = createTestBook();
        int totalCopies = 3 + random.nextInt(3); // 3-5 copies
        List<PublicationCopy> copies = createMultipleCopies(book, totalCopies);
        
        // Borrow some copies randomly
        int copiesToBorrow = 1 + random.nextInt(totalCopies - 1); // Borrow 1 to (totalCopies-1)
        for (int i = 0; i < copiesToBorrow; i++) {
            User user = createTestUser();
            borrowService.borrowBook(user.getUserId(), book.getBookId());
        }
        
        // Action: Get available copies
        List<PublicationCopy> availableCopies = publicationCopyRepository.findByPublicationAndIsAvailable(book, true);
        
        // Verification: Check that available copies are displayed correctly
        int expectedAvailable = totalCopies - copiesToBorrow;
        assertEquals(expectedAvailable, availableCopies.size(),
            String.format("Πρέπει να εμφανίζονται %d διαθέσιμα αντίτυπα", expectedAvailable));
        
        // Verify all returned copies are actually available
        for (PublicationCopy copy : availableCopies) {
            assertTrue(copy.getIsAvailable(), 
                String.format("Το Copy #%d πρέπει να είναι isAvailable=true", copy.getCopyNumber()));
        }
    }

    /**
     * Property 2: Preservation - Test 3: Δημιουργία Ξεχωριστών Εγγραφών για Διαφορετικά Βιβλία
     * 
     * Επαληθεύει ότι όταν ένας χρήστης δανείζεται διαφορετικά βιβλία,
     * το σύστημα δημιουργεί ξεχωριστές εγγραφές δανεισμού για κάθε βιβλίο.
     * 
     * **Validates: Requirements 3.3**
     */
    @RepeatedTest(10)
    void preservation_separateRecordsForDifferentBooks_worksCorrectly() {
        // Setup: Create user and multiple books
        User user = createTestUser();
        int numberOfBooks = 2 + random.nextInt(3); // 2-4 books
        List<Book> books = new ArrayList<>();
        for (int i = 0; i < numberOfBooks; i++) {
            Book book = createTestBook();
            createMultipleCopies(book, 1); // Each book has 1 copy
            books.add(book);
        }
        
        // Action: Borrow all books
        List<Borrow> borrows = new ArrayList<>();
        for (Book book : books) {
            Optional<Borrow> result = borrowService.borrowBook(user.getUserId(), book.getBookId());
            assertTrue(result.isPresent(), "Ο δανεισμός πρέπει να είναι επιτυχής");
            borrows.add(result.get());
        }
        
        // Verification: Check that separate records are created
        List<Borrow> activeBorrows = borrowRepository.findByUserAndReturned(user, 0);
        assertEquals(numberOfBooks, activeBorrows.size(),
            String.format("Πρέπει να δημιουργηθούν %d ξεχωριστές εγγραφές δανεισμού", numberOfBooks));
        
        // Verify each borrow is for a different book
        List<Long> borrowedBookIds = activeBorrows.stream()
            .map(b -> b.getPublicationCopy().getBook().getBookId())
            .distinct()
            .collect(Collectors.toList());
        assertEquals(numberOfBooks, borrowedBookIds.size(),
            "Κάθε εγγραφή δανεισμού πρέπει να αφορά διαφορετικό βιβλίο");
    }

    /**
     * Property 2: Preservation - Test 4: Ενημέρωση isAvailable
     * 
     * Επαληθεύει ότι τα αντίτυπα ενημερώνονται σωστά κατά τον δανεισμό και την επιστροφή.
     * - Κατά τον δανεισμό: isAvailable=false
     * - Κατά την επιστροφή: isAvailable=true
     * 
     * **Validates: Requirements 3.4**
     */
    @RepeatedTest(10)
    void preservation_isAvailableUpdate_worksCorrectly() throws Exception {
        // Setup: Create user and book
        User user = createTestUser();
        Book book = createTestBook();
        List<PublicationCopy> copies = createMultipleCopies(book, 1);
        PublicationCopy copy = copies.get(0);
        Long copyId = copy.getCopyId();
        
        // Verify initial state
        assertTrue(copy.getIsAvailable(), "Το αντίτυπο πρέπει να είναι αρχικά διαθέσιμο");
        
        // Action 1: Borrow the book
        Optional<Borrow> borrowResult = borrowService.borrowBook(user.getUserId(), book.getBookId());
        assertTrue(borrowResult.isPresent(), "Ο δανεισμός πρέπει να είναι επιτυχής");
        
        // Verification 1: Copy should be unavailable after borrowing
        PublicationCopy borrowedCopy = publicationCopyRepository.findById(copyId).orElseThrow();
        assertFalse(borrowedCopy.getIsAvailable(),
            "Το αντίτυπο πρέπει να είναι μη διαθέσιμο μετά τον δανεισμό (isAvailable=false)");
        
        // Action 2: Return the book
        Optional<Borrow> returnResult = borrowService.returnBook(user.getUserId(), book.getBookId());
        assertTrue(returnResult.isPresent(), "Η επιστροφή πρέπει να είναι επιτυχής");
        
        // Verification 2: Copy should be available after returning
        PublicationCopy returnedCopy = publicationCopyRepository.findById(copyId).orElseThrow();
        assertTrue(returnedCopy.getIsAvailable(),
            "Το αντίτυπο πρέπει να είναι διαθέσιμο μετά την επιστροφή (isAvailable=true)");
    }

    /**
     * Property 2: Preservation - Test 5: Φόρτωση Λίστας Δανεισμών
     * 
     * Επαληθεύει ότι οι getActiveBorrows() και getBorrowHistory() συνεχίζουν να λειτουργούν σωστά.
     * Το σύστημα πρέπει να εμφανίζει σωστά τους ενεργούς και ολοκληρωμένους δανεισμούς.
     * 
     * **Validates: Requirements 3.5**
     */
    @RepeatedTest(10)
    void preservation_borrowListLoading_worksCorrectly() throws Exception {
        // Setup: Create users and books
        int numberOfBorrows = 2 + random.nextInt(3); // 2-4 borrows
        List<User> users = new ArrayList<>();
        List<Book> books = new ArrayList<>();
        
        for (int i = 0; i < numberOfBorrows; i++) {
            User user = createTestUser();
            Book book = createTestBook();
            createMultipleCopies(book, 1);
            users.add(user);
            books.add(book);
        }
        
        // Action 1: Create borrows
        for (int i = 0; i < numberOfBorrows; i++) {
            borrowService.borrowBook(users.get(i).getUserId(), books.get(i).getBookId());
        }
        
        // Verification 1: Check active borrows
        List<Borrow> activeBorrows = borrowService.getActiveBorrows();
        assertTrue(activeBorrows.size() >= numberOfBorrows,
            String.format("Πρέπει να υπάρχουν τουλάχιστον %d ενεργοί δανεισμοί", numberOfBorrows));
        
        // Action 2: Return some books
        int borrowsToReturn = 1 + random.nextInt(numberOfBorrows);
        for (int i = 0; i < borrowsToReturn; i++) {
            borrowService.returnBook(users.get(i).getUserId(), books.get(i).getBookId());
        }
        
        // Verification 2: Check borrow history
        List<Borrow> borrowHistory = borrowService.getBorrowHistory();
        assertTrue(borrowHistory.size() >= borrowsToReturn,
            String.format("Πρέπει να υπάρχουν τουλάχιστον %d επιστραμμένοι δανεισμοί στο ιστορικό", borrowsToReturn));
        
        // Verify all history items are returned
        for (Borrow borrow : borrowHistory) {
            assertEquals(1, borrow.getReturned(),
                "Όλες οι εγγραφές στο ιστορικό πρέπει να είναι επιστραμμένες (returned=1)");
        }
    }

    /**
     * Property 2: Preservation - Test 6: Πολλαπλοί Δανεισμοί του Ίδιου Βιβλίου
     * 
     * Επαληθεύει ότι όταν ένας χρήστης δανείζεται το ίδιο βιβλίο (διαφορετικό αντίτυπο)
     * πολλαπλές φορές, το σύστημα δημιουργεί ξεχωριστές εγγραφές για κάθε δανεισμό.
     * 
     * **Validates: Requirements 3.6**
     */
    @RepeatedTest(10)
    void preservation_multipleBorrowsOfSameBook_worksCorrectly() {
        // Setup: Create user and book with multiple copies
        User user = createTestUser();
        Book book = createTestBook();
        int numberOfCopies = 2 + random.nextInt(3); // 2-4 copies
        List<PublicationCopy> copies = createMultipleCopies(book, numberOfCopies);
        
        // Action: Borrow multiple copies of the same book
        List<Borrow> borrows = new ArrayList<>();
        for (int i = 0; i < numberOfCopies; i++) {
            Optional<Borrow> result = borrowService.borrowBook(user.getUserId(), book.getBookId());
            assertTrue(result.isPresent(), 
                String.format("Ο δανεισμός %d/%d πρέπει να είναι επιτυχής", i + 1, numberOfCopies));
            borrows.add(result.get());
        }
        
        // Verification: Check that separate records are created
        List<Borrow> activeBorrows = borrowRepository.findByUserAndReturned(user, 0);
        assertEquals(numberOfCopies, activeBorrows.size(),
            String.format("Πρέπει να δημιουργηθούν %d ξεχωριστές εγγραφές δανεισμού", numberOfCopies));
        
        // Verify each borrow is for a different copy
        List<Long> borrowedCopyIds = activeBorrows.stream()
            .map(b -> b.getPublicationCopy().getCopyId())
            .distinct()
            .collect(Collectors.toList());
        assertEquals(numberOfCopies, borrowedCopyIds.size(),
            "Κάθε εγγραφή δανεισμού πρέπει να αφορά διαφορετικό αντίτυπο");
        
        // Verify all copies are marked as unavailable
        for (PublicationCopy copy : copies) {
            PublicationCopy updatedCopy = publicationCopyRepository.findById(copy.getCopyId()).orElseThrow();
            assertFalse(updatedCopy.getIsAvailable(),
                String.format("Το Copy #%d πρέπει να είναι μη διαθέσιμο (isAvailable=false)", 
                    copy.getCopyNumber()));
        }
    }

    @AfterEach
    void cleanup() {
        // Delete borrows first (due to foreign key constraints)
        for (Long userId : createdUserIds) {
            userRepository.findById(userId).ifPresent(user -> {
                borrowRepository.deleteAll(borrowRepository.findByUser(user));
            });
        }
        
        // Delete copies
        for (Long copyId : createdCopyIds) {
            publicationCopyRepository.deleteById(copyId);
        }
        
        // Delete books
        for (Long bookId : createdBookIds) {
            bookRepository.deleteById(bookId);
        }
        
        // Delete authors
        for (Long authorId : createdAuthorIds) {
            authorRepository.deleteById(authorId);
        }
        
        // Delete publishers
        for (Long publisherId : createdPublisherIds) {
            publisherRepository.deleteById(publisherId);
        }
        
        // Delete users
        for (Long userId : createdUserIds) {
            userRepository.deleteById(userId);
        }
        
        // Clear lists
        createdUserIds.clear();
        createdBookIds.clear();
        createdCopyIds.clear();
        createdAuthorIds.clear();
        createdPublisherIds.clear();
    }
}
