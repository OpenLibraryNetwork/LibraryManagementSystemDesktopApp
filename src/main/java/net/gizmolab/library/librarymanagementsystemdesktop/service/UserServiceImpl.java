package net.gizmolab.library.librarymanagementsystemdesktop.service;

import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.BorrowRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.repository.UserRepository;
import net.gizmolab.library.librarymanagementsystemdesktop.service.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final BorrowRepository borrowRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BorrowRepository borrowRepository) {
        this.userRepository = userRepository;
        this.borrowRepository = borrowRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByLastnameAscFirstnameAsc();
    }

    @Override
    public Page<User> getAllUsersWithPagination(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("lastname"), Sort.Order.asc("firstname")));
        return userRepository.findAllByOrderByLastnameAscFirstnameAsc(pageable);
    }

    @Override
    public User getUserByPhone(String phone) throws EntityNotFoundException {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public User getUserById(Long userId) throws EntityNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    @Override
    public User updateUser(Long userId, User user) throws EntityNotFoundException {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        // Update only editable fields, preserve relationships
        existing.setFirstname(user.getFirstname());
        existing.setLastname(user.getLastname());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        return userRepository.save(existing);
    }

    @Transactional
    @Override
    public void deleteUser(Long userId) throws EntityNotFoundException {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found");
        }
        userRepository.deleteById(userId);
    }

    @Override
    public List<User> getAllUsersWithActiveBorrows() {
        List<User> users = userRepository.findAll();

        // Single query for all active borrow counts
        List<Object[]> counts = borrowRepository.countActiveBorrowsByUser();
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : counts) {
            countMap.put((Long) row[0], (Long) row[1]);
        }

        for (User user : users) {
            Long count = countMap.getOrDefault(user.getUserId(), 0L);
            user.setActiveBorrowCount(count.intValue());
        }
        return users;
    }

    public Long countUsers() {
        return userRepository.count();
    }


}
