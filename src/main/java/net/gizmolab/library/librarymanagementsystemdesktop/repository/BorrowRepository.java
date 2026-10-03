package net.gizmolab.library.librarymanagementsystemdesktop.repository;

import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for local Borrow records.
 * All queries use local H2 fields — no JPA joins to Strapi entities.
 */
@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {

    List<Borrow> findByUserAndReturned(User user, boolean returned);


    // === By return status ===
    List<Borrow> findByReturned(boolean returned);


    @Query("SELECT COUNT(b) FROM Borrow b")
    Long countAllBorrows();

    @Query("SELECT COUNT(b) FROM Borrow b WHERE b.returned = false")
    Long countActiveBorrows();

    @Query("SELECT b.user.userId, COUNT(b) FROM Borrow b WHERE b.returned = false GROUP BY b.user.userId")
    List<Object[]> countActiveBorrowsByUser();

    // === Fetch with User (for lists); LEFT: anonymised borrows have no user but still count ===
    @Query("SELECT b FROM Borrow b LEFT JOIN FETCH b.user")
    List<Borrow> findAllWithUser();

    @Query("SELECT b FROM Borrow b JOIN FETCH b.user WHERE b.returned = false")
    List<Borrow> findActiveWithUser();

    // === Privacy: unlink borrowers from returned borrows ===
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Borrow b SET b.user = null WHERE b.returned = true AND b.returnDate < :cutoff AND b.user IS NOT NULL")
    int detachBorrowersReturnedBefore(LocalDate cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Borrow b SET b.user = null WHERE b.returned = true AND b.user = :user")
    int detachBorrowerFromReturned(User user);

}
