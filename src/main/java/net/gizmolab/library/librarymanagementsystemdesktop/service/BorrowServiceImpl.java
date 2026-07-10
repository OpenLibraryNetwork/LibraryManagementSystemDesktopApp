package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.dto.BorrowDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.CopyDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.PublicationDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.model.Borrow;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.BorrowRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.UserRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.service.exceptions.EntityNotFoundException;
import net.gizmolab.library.librarymanagementsystemdesktop.service.utilities.DTOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Borrow service — orchestrates Strapi + local H2.
 *
 * BORROW flow:
 * 1. Strapi: POST /api/copies/borrow → isAvailable=false (atomic)
 * 2. H2: Create local Borrow record (with cached fields for offline display)
 *
 * RETURN flow:
 * 1. Strapi: POST /api/copies/return → isAvailable=true (atomic)
 * 2. H2: Update local Borrow record (returned=true, returnDate=now)
 */
@Service
public class BorrowServiceImpl implements IBorrowService {

    private final BorrowRepository borrowRepository;
    private final UserRepository userRepository;
    private final StrapiApiClient strapiApiClient;

    @Autowired
    public BorrowServiceImpl(BorrowRepository borrowRepository,
                             UserRepository userRepository,
                             StrapiApiClient strapiApiClient) {
        this.borrowRepository = borrowRepository;
        this.userRepository = userRepository;
        this.strapiApiClient = strapiApiClient;
    }

    @Transactional
    @Override
    public Borrow borrowItem(User user, CopyDTO copy, PublicationDTO pub, LocalDate dueDate) {
        // 1. Strapi: atomic borrow (sets isAvailable=false)
        // Throws ConflictException if already borrowed
        // Throws AuthenticationExpiredException if 401
        try {
            strapiApiClient.borrowCopy(copy.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to borrow copy from Strapi: " + e.getMessage(), e);
        }

        // 2. H2: Create local Borrow record with cached fields
        Borrow borrow = new Borrow();
        borrow.setUser(user);
        borrow.setStrapiCopyId(copy.getId());
        borrow.setStrapiPublicationId(pub.getId());
        borrow.setCopyNumber(copy.getCopyNumber());
        borrow.setPublicationTitle(pub.getTitle());
        borrow.setPublicationType(pub.getType());
        borrow.setIsbn(pub.getIsbn());
        borrow.setAuthorName(pub.getAuthorNames());
        borrow.setBorrowDate(LocalDate.now());
        borrow.setDueDate(dueDate);
        borrow.setReturned(false);

        return borrowRepository.save(borrow);
    }

    @Transactional
    @Override
    public Borrow returnItem(Long borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new EntityNotFoundException("Borrow not found: " + borrowId));

        // 1. Strapi: atomic return (sets isAvailable=true)
        try {
            strapiApiClient.returnCopy(borrow.getStrapiCopyId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to return copy to Strapi: " + e.getMessage(), e);
        }

        // 2. H2: Update local record
        borrow.setReturned(true);
        borrow.setReturnDate(LocalDate.now());

        return borrowRepository.save(borrow);
    }

    @Override
    public List<Borrow> getActiveBorrows() {
        return borrowRepository.findByReturned(false);
    }

    @Override
    public List<Borrow> getActiveBorrowsByUser(User user) {
        return borrowRepository.findByUserAndReturned(user, false);
    }

    @Override
    public List<Borrow> getBorrowHistory() {
        return borrowRepository.findByReturned(true);
    }

    @Override
    public List<Borrow> getBorrowHistoryByUser(User user) {
        return borrowRepository.findByUserAndReturned(user, true);
    }

    @Override
    public Long countAllBorrows() {
        return borrowRepository.countAllBorrows();
    }

    @Override
    public Long countActiveBorrows() {
        return borrowRepository.countActiveBorrows();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDTO> getAllBorrowsAsDTO() {
        return borrowRepository.findAllWithUser().stream()
                .map(DTOConverter::convertBorrowToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDTO> getActiveBorrowsAsDTO() {
        return borrowRepository.findActiveWithUser().stream()
                .map(DTOConverter::convertBorrowToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDTO> getBorrowHistoryAsDTO() {
        return getBorrowHistory().stream()
                .map(DTOConverter::convertBorrowToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDTO> getBorrowHistoryByUserAsDTO(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        return getBorrowHistoryByUser(user).stream()
                .map(DTOConverter::convertBorrowToDTO)
                .collect(Collectors.toList());
    }
}
