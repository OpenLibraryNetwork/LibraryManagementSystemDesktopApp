package net.gizmolab.library.librarymanagementsystemdesktop.repository;

import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for local Borrow records.
 * All queries use local H2 fields — no JPA joins to Strapi entities.
 */
@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {

    // === By User ===
    List<Borrow> findByUser(User user);
    List<Borrow> findByUserAndReturned(User user, boolean returned);

    // === By Strapi Copy ID ===
    Optional<Borrow> findByUserAndStrapiCopyDocumentIdAndReturned(User user, String strapiCopyDocumentId, boolean returned);
    List<Borrow> findByStrapiCopyDocumentIdAndReturned(String strapiCopyDocumentId, boolean returned);

    // === By return status ===
    List<Borrow> findByReturned(boolean returned);

    // === Counts ===
    int countByUserAndReturned(User user, boolean returned);

    @Query("SELECT COUNT(b) FROM Borrow b")
    Long countAllBorrows();

    @Query("SELECT COUNT(b) FROM Borrow b WHERE b.returned = false")
    Long countActiveBorrows();

    @Query("SELECT b.user.userId, COUNT(b) FROM Borrow b WHERE b.returned = false GROUP BY b.user.userId")
    List<Object[]> countActiveBorrowsByUser();

    // === Fetch with User (for lists) ===
    @Query("SELECT b FROM Borrow b JOIN FETCH b.user")
    List<Borrow> findAllWithUser();

    @Query("SELECT b FROM Borrow b JOIN FETCH b.user WHERE b.returned = false")
    List<Borrow> findActiveWithUser();

    @Query("SELECT b FROM Borrow b JOIN FETCH b.user WHERE b.user = :user AND b.returned = false")
    List<Borrow> findActiveByUser(@Param("user") User user);
}
