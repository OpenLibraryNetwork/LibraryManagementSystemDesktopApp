package net.gizmolab.library.librarymanagementsystemdesktop.service.utilities;

import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Utility for borrow-related operations.
 * Simplified — book/copy lookups now go through StrapiApiClient.
 */
@Service
public class BorrowUtility {

    @Autowired
    private UserRepository userRepository;

    public User retrieveUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
