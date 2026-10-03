package com.netpulse.user.service;

import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.user.dto.UserResponse;
import com.netpulse.user.entity.Role;
import com.netpulse.user.entity.User;
import com.netpulse.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Alice Engineer", "alice@netpulse.io", "hashedPass", Role.ADMIN);
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("Should successfully retrieve user by email")
    void testGetUserByEmailSuccess() {
        when(userRepository.findByEmail("alice@netpulse.io")).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserByEmail("alice@netpulse.io");

        assertNotNull(response);
        assertEquals("Alice Engineer", response.getName());
        assertEquals("alice@netpulse.io", response.getEmail());
        assertEquals(Role.ADMIN, response.getRole());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user email does not exist")
    void testGetUserByEmailNotFound() {
        when(userRepository.findByEmail("unknown@netpulse.io")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("unknown@netpulse.io"));
    }

    @Test
    @DisplayName("New User entity should default emailVerified to false")
    void testUserEntityDefaultEmailVerifiedFalse() {
        User user = new User("Charlie", "charlie@netpulse.io", "pass", Role.OPERATOR);
        assertFalse(user.isEmailVerified());

        user.setEmailVerified(true);
        assertTrue(user.isEmailVerified());
    }
}
