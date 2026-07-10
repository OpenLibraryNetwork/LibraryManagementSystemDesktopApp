package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.CopyDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;

import java.time.LocalDate;
import java.util.List;

public interface IBorrowService {

    /** Borrow a copy (online — calls Strapi + saves local record). */
    Borrow borrowItem(User user, CopyDTO copy, PublicationDTO pub, LocalDate dueDate);

    /** Return a borrowed item (online — calls Strapi + updates local record). */
    Borrow returnItem(Long borrowId);

    /** Get all active borrows (local H2). */
    List<Borrow> getActiveBorrows();

    /** Get active borrows for a specific user. */
    List<Borrow> getActiveBorrowsByUser(User user);

    /** Get borrow history (returned items). */
    List<Borrow> getBorrowHistory();

    /** Get borrow history for a specific user. */
    List<Borrow> getBorrowHistoryByUser(User user);

    // Counts
    Long countAllBorrows();
    Long countActiveBorrows();

    // DTO methods for UI
    List<BorrowDTO> getAllBorrowsAsDTO();
    List<BorrowDTO> getActiveBorrowsAsDTO();
    List<BorrowDTO> getBorrowHistoryAsDTO();
    List<BorrowDTO> getBorrowHistoryByUserAsDTO(Long userId);
}
