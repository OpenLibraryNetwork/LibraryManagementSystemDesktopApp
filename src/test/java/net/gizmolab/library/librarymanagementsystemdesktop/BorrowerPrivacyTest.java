package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.BorrowRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.UserRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.service.BorrowRetentionService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IBorrowService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import net.gizmolab.library.librarymanagementsystemdesktop.service.exceptions.ActiveBorrowsException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GDPR storage limitation: a returned borrow keeps its borrower for 30 days (for disputes such as
 * "I did return it"), then only the publication and the dates stay, so the popularity statistics survive.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BorrowerPrivacyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    @Autowired private BorrowRetentionService retention;
    @Autowired private BorrowRepository borrows;
    @Autowired private UserRepository users;
    @Autowired private IUserService userService;
    @Autowired private IBorrowService borrowService;
    @Autowired private EntityManager em;

    private User borrower(String phone) {
        User u = new User();
        u.setFirstname("Μαρία");
        u.setLastname("Παπαδοπούλου");
        u.setEmail("maria@example.org");
        u.setPhone(phone);
        return users.save(u);
    }

    private Borrow borrow(User user, LocalDate returnedOn) {
        Borrow b = new Borrow();
        b.setUser(user);
        b.setStrapiCopyDocumentId("copy-" + System.nanoTime());
        b.setStrapiPublicationDocumentId("pub-1");
        b.setPublicationTitle("Κοινωνικός Αναρχισμός");
        b.setBorrowDate(TODAY.minusDays(60));
        b.setDueDate(TODAY.minusDays(46));
        b.setReturned(returnedOn != null);
        b.setReturnDate(returnedOn);
        return borrows.save(b);
    }

    private Borrow reload(Borrow b) {
        em.flush();
        em.clear();
        return borrows.findById(b.getId()).orElseThrow();
    }

    @Test
    void aBorrowReturnedMoreThan30DaysAgoNoLongerNamesItsBorrower() {
        User maria = borrower("6900000001");
        Borrow old = borrow(maria, TODAY.minusDays(31));
        Borrow recent = borrow(maria, TODAY.minusDays(30));
        Borrow active = borrow(maria, null);

        assertEquals(1, retention.anonymizeReturnedBefore(TODAY));

        assertNull(reload(old).getUser());
        assertNotNull(reload(recent).getUser());
        assertNotNull(reload(active).getUser());
    }

    @Test
    void anAnonymousBorrowStillCountsInTheStatistics() {
        Borrow old = borrow(borrower("6900000002"), TODAY.minusDays(90));
        retention.anonymizeReturnedBefore(TODAY);
        em.flush();
        em.clear();

        assertTrue(borrowService.getAllBorrowsAsDTO().stream()
                .anyMatch(dto -> dto.getId().equals(old.getId()) && dto.getUserFullName().isEmpty()));
    }

    @Test
    void aBorrowerWithABookAtHomeCannotBeDeleted() {
        User maria = borrower("6900000003");
        borrow(maria, null);
        em.flush();

        Long id = maria.getUserId();
        assertThrows(ActiveBorrowsException.class, () -> userService.deleteUser(id));
        assertTrue(users.existsById(id));
    }

    @Test
    void deletingABorrowerKeepsTheirReturnedBorrowsAnonymously() {
        User maria = borrower("6900000004");
        Borrow returned = borrow(maria, TODAY.minusDays(3));
        em.flush();
        em.clear();

        userService.deleteUser(maria.getUserId());

        assertFalse(users.existsById(maria.getUserId()));
        assertNull(reload(returned).getUser());
    }

    @Test
    void logLinesNeverCarryContactDetails() {
        User maria = borrower("6900000005");
        Borrow b = borrow(maria, null);
        String text = maria + " " + b;
        assertFalse(text.contains("6900000005"), text);
        assertFalse(text.contains("maria@example.org"), text);
        assertFalse(text.contains("Παπαδοπούλου"), text);
    }
}
