package net.gizmolab.library.librarymanagementsystemdesktop;

import net.gizmolab.library.librarymanagementsystemdesktop.controller.UserManagementController;
import net.gizmolab.library.librarymanagementsystemdesktop.dto.UserDTO;
import net.gizmolab.library.librarymanagementsystemdesktop.model.User;
import net.gizmolab.library.librarymanagementsystemdesktop.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserManagementIntegrationTest {

    @Autowired
    private IUserService userService;

    @Autowired
    private UserManagementController userManagementController;

    @Test
    public void testUserServiceIntegration() {
        // Test that we can create and retrieve users
        User user = new User();
        user.setFirstname("John");
        user.setLastname("Doe");
        user.setEmail("john.doe@example.com");
        user.setPhone("1234567890");

        User savedUser = userService.createUser(user);
        assertNotNull(savedUser);
        assertNotNull(savedUser.getUserId());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastname());
        assertEquals("john.doe@example.com", savedUser.getEmail());
        assertEquals("1234567890", savedUser.getPhone());

        // Test retrieval
        List<User> users = userService.getAllUsers();
        assertTrue(users.size() > 0);
        
        // Test that we can find our user
        boolean found = users.stream()
                .anyMatch(u -> "John".equals(u.getFirstname()) && "Doe".equals(u.getLastname()));
        assertTrue(found);
    }

    @Test
    public void testUserManagementControllerExists() {
        // Test that the controller is properly injected
        assertNotNull(userManagementController);
    }

    @Test
    public void testUserDTOConversion() {
        // Test UserDTO functionality
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstname("Jane");
        userDTO.setLastname("Smith");
        userDTO.setEmail("jane.smith@example.com");
        userDTO.setPhone("0987654321");
        userDTO.setActiveBorrowCount(2);

        assertEquals("Jane", userDTO.getFirstname());
        assertEquals("Smith", userDTO.getLastname());
        assertEquals("jane.smith@example.com", userDTO.getEmail());
        assertEquals("0987654321", userDTO.getPhone());
        assertEquals(2, userDTO.getActiveBorrowCount());
        assertEquals("Jane Smith", userDTO.getFullName());
    }
}