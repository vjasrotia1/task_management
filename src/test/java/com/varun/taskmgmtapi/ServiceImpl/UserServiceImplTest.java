package com.varun.taskmgmtapi.ServiceImpl;
import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRoleRequest;
import com.varun.taskmgmtapi.exception.UserNotFoundException;
import com.varun.taskmgmtapi.exception.LastAdminException;
import com.varun.taskmgmtapi.models.Role;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.UserRepo;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserServiceImpl userService;


    // Helper method to create a mocked User
    private User createMockUser(Long id, Role role) {

        User user = mock(User.class);

        //when(user.getId()).thenReturn(id);
        //when(user.getName()).thenReturn("Test User");
        //when(user.getEmail()).thenReturn("test@example.com");
        when(user.getRole()).thenReturn(role);

        return user;
    }


    // TEST 1: ADMIN promotes USER to ADMIN
    @Test
    void updateUserRole_shouldPromoteUserToAdmin() {

        // Arrange
        Long userId = 2L;
        Long currentAdminId = 1L;

        User user = createMockUser(userId, Role.USER);

        when(userRepo.findById(userId))
                .thenReturn(Optional.of(user));

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.ADMIN);


        // Act
        UserResponse response = userService.updateUserRole(
                userId, request, currentAdminId);


        // Assert
        assertNotNull(response);

        // Verify
        verify(user).setRole(Role.ADMIN);
        verify(userRepo, never()).countByRole(Role.ADMIN);
        verify(userRepo, never()).save(any(User.class));
    }


    // TEST 2: ADMIN demotes another ADMIN when multiple admins exist
    @Test
    void updateUserRole_shouldDemoteAnotherAdminWhenMultipleAdminsExist() {

        // Arrange
        Long userId = 2L;
        Long currentAdminId = 1L;

        User user = createMockUser(userId, Role.ADMIN);

        when(userRepo.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepo.countByRole(Role.ADMIN))
                .thenReturn(2L);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.USER);


        // Act
        UserResponse response = userService.updateUserRole(
                userId, request, currentAdminId);


        // Assert
        assertNotNull(response);

        // Verify
        verify(userRepo).countByRole(Role.ADMIN);
        verify(user).setRole(Role.USER);
    }


    // TEST 3: ADMIN demotes themselves when another ADMIN exists
    @Test
    void updateUserRole_shouldAllowAdminToDemoteThemselvesWhenAnotherAdminExists() {

        // Arrange
        Long userId = 1L;
        Long currentAdminId = 1L;

        User user = createMockUser(userId, Role.ADMIN);

        when(userRepo.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepo.countByRole(Role.ADMIN))
                .thenReturn(2L);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.USER);


        // Act
        UserResponse response = userService.updateUserRole(
                userId, request, currentAdminId);


        // Assert
        assertNotNull(response);

        // Verify
        verify(userRepo).countByRole(Role.ADMIN);
        verify(user).setRole(Role.USER);
    }


    // TEST 4: Prevent demotion of the last ADMIN
    @Test
    void updateUserRole_shouldThrowExceptionWhenDemotingLastAdmin() {

        // Arrange
        Long userId = 1L;
        Long currentAdminId = 1L;

        User user = createMockUser(userId, Role.ADMIN);

        when(userRepo.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepo.countByRole(Role.ADMIN))
                .thenReturn(1L);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.USER);


        // Act + Assert
        assertThrows(LastAdminException.class, () ->
                userService.updateUserRole(
                        userId, request, currentAdminId));


        // Verify
        verify(userRepo).countByRole(Role.ADMIN);
        verify(user, never()).setRole(any(Role.class));
    }


    // TEST 5: No update when requested role is already assigned
    @Test
    void updateUserRole_shouldNotUpdateWhenRoleIsAlreadySame() {

        // Arrange
        Long userId = 2L;
        Long currentAdminId = 1L;

        User user = createMockUser(userId, Role.USER);

        when(userRepo.findById(userId))
                .thenReturn(Optional.of(user));

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.USER);


        // Act
        UserResponse response = userService.updateUserRole(
                userId, request, currentAdminId);


        // Assert
        assertNotNull(response);

        // Verify
        verify(user, never()).setRole(any(Role.class));
        verify(userRepo, never()).countByRole(Role.ADMIN);
    }


    // TEST 6: User ID does not exist
    @Test
    void updateUserRole_shouldThrowExceptionWhenUserDoesNotExist() {

        // Arrange
        Long userId = 99L;
        Long currentAdminId = 1L;

        when(userRepo.findById(userId))
                .thenReturn(Optional.empty());

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.ADMIN);


        // Act + Assert
        assertThrows(UserNotFoundException.class, () ->
                userService.updateUserRole(
                        userId, request, currentAdminId));


        // Verify
        verify(userRepo, never()).countByRole(Role.ADMIN);
    }
}
/*
Understand two important tests
Test 2: Demoting another ADMIN
when(userRepo.countByRole(Role.ADMIN))
        .thenReturn(2L);


We simulate a database containing two ADMIN users.

The service checks the count, sees 2, and allows the demotion.

Test 4: Demoting the last ADMIN
when(userRepo.countByRole(Role.ADMIN))
        .thenReturn(1L);


We simulate a database containing only one ADMIN.

The service throws LastAdminException, and we verify:
verify(user, never()).setRole(any(Role.class));


This confirms that the role was not changed.
 */
