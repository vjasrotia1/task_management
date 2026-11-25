package com.varun.taskmgmtapi.ServiceImpl;
import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRoleRequest;
import com.varun.taskmgmtapi.exception.LastAdminException;
import com.varun.taskmgmtapi.models.Role;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("mysql-test")
public class UserRoleConcurrencyIntegrationTest {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private UserServiceImpl userService;

    private User admin1;
    private User admin2;

    @BeforeEach
    void setUp() {

        userRepo.deleteAll();

        admin1 = createAdmin("admin1@test.com");
        admin2 = createAdmin("admin2@test.com");

        admin1 = userRepo.saveAndFlush(admin1);
        admin2 = userRepo.saveAndFlush(admin2);
    }

    private User createAdmin(String email) {

        User user = new User();

        user.setName("Test Admin");
        user.setEmail(email);
        user.setPassword("dummy-password");
        user.setRole(Role.ADMIN);

        return user;
    }

    @Test
    void concurrentDemotions_shouldNeverRemoveLastAdmin() throws Exception {

        // Both threads will wait here until we release them.
        CountDownLatch startSignal = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            // Thread 1 attempts to demote Admin 1.
            Future<Boolean> firstResult = executor.submit(() -> {

                startSignal.await();

                return tryDemoteAdmin(admin1);
            });

            // Thread 2 attempts to demote Admin 2.
            Future<Boolean> secondResult = executor.submit(() -> {

                startSignal.await();

                return tryDemoteAdmin(admin2);
            });

            // Release both threads.
            startSignal.countDown();

            boolean firstSucceeded = firstResult.get();
            boolean secondSucceeded = secondResult.get();

            // Exactly one demotion should succeed.
            assertNotEquals(firstSucceeded, secondSucceeded);

            // Exactly one ADMIN must remain.
            assertEquals(1, userRepo.countByRole(Role.ADMIN));

            // The other user must have become USER.
            assertEquals(1, userRepo.countByRole(Role.USER));

        } finally {

            executor.shutdownNow();
        }
    }

    private boolean tryDemoteAdmin(User admin) {

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.USER);

        try {

            userService.updateUserRole(
                    admin.getId(),
                    request,
                    admin.getId()
            );

            return true;

        } catch (LastAdminException exception) {

            return false;
        }
    }

}

/*
Understand what this test does
1. Prepare the database

   @BeforeEach creates two ADMIN users in your dedicated MySQL database.
2. Create two threads

   Each thread attempts to demote a different ADMIN.
3. Release both threads together

   CountDownLatch makes both threads wait until the test releases them.
4. Let the service enforce locking

   Both calls execute the real UserServiceImpl method, which obtains pessimistic locks through MySQL.
5. Verify the outcome

   Exactly one demotion succeeds; the other receives LastAdminException.
Important: Do not add @Transactional to this test class or test method. Each service invocation must run in its own transaction so that the database locks are independently acquired and released.
 */
