package net.gizmolab.library.librarymanagementsystemdesktop.service;

import jakarta.persistence.EntityManager;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.BorrowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * GDPR storage limitation. What someone borrowed can reveal their beliefs, so a returned borrow keeps its
 * borrower only for borrow.history.retention.days (default 30: time for "I did return it" disputes).
 * After that only the publication and the dates stay, which is all the statistics need.
 */
@Service
public class BorrowRetentionService {

    private static final Logger log = LoggerFactory.getLogger(BorrowRetentionService.class);

    private final BorrowRepository borrowRepository;
    private final EntityManager entityManager;
    private final int retentionDays;

    public BorrowRetentionService(BorrowRepository borrowRepository, EntityManager entityManager,
                                  @Value("${borrow.history.retention.days:30}") int retentionDays) {
        this.borrowRepository = borrowRepository;
        this.entityManager = entityManager;
        this.retentionDays = retentionDays;
    }

    /** Runs at every start of the application. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        // Databases created before anonymisation have USER_ID NOT NULL; ddl-auto=update does not relax it
        entityManager.createNativeQuery("ALTER TABLE BORROWS ALTER COLUMN USER_ID SET NULL").executeUpdate();
        int count = borrowRepository.detachBorrowersReturnedBefore(cutoff(LocalDate.now(ZoneId.systemDefault())));
        if (count > 0) log.info("Anonymised {} borrows returned more than {} days ago", count, retentionDays);
    }

    /** Unlinks the borrower from every borrow returned more than retentionDays before today; returns how many. */
    @Transactional
    public int anonymizeReturnedBefore(LocalDate today) {
        return borrowRepository.detachBorrowersReturnedBefore(cutoff(today));
    }

    private LocalDate cutoff(LocalDate today) {
        return today.minusDays(retentionDays);
    }
}
