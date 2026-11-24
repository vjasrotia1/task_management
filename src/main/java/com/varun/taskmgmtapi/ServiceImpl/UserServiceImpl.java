package com.varun.taskmgmtapi.ServiceImpl;

import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRoleRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.exception.LastAdminException;
import com.varun.taskmgmtapi.exception.UserNotFoundException;
import com.varun.taskmgmtapi.models.Role;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.UserRepo;
import com.varun.taskmgmtapi.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private UserRepo userRepo;
    public UserServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long userId, UpdateUserRoleRequest updateUserRoleRequest, Long currentAdminId) {
        User user=userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

        // Check whether the requested role is the same
        if (user.getRole() == updateUserRoleRequest.getRole()) {
            return convertUserToUserResponse(user);
        }

        // Check whether an ADMIN is being demoted
        if (user.getRole() == Role.ADMIN
                && updateUserRoleRequest.getRole() == Role.USER) {

            long adminCount = userRepo.countByRole(Role.ADMIN);

            if (adminCount <= 1) {
                throw new LastAdminException(
                        "Cannot demote the last ADMIN user"
                );
                //This may result in HTTP 500 unless your global exception handler maps it appropriately.
                //A better approach is to create a custom exception:LastAdminException

            }
        }

        user.setRole(updateUserRoleRequest.getRole());

        return convertUserToUserResponse(user);

    }

    private UserResponse convertUserToUserResponse(User user){
        UserResponse userResponse=new UserResponse(
                user.getId(), user.getName(),user.getEmail(),user.getRole()
        );
        return userResponse;

        //The problem is that it changes the role without checking the target user's existing role or the number of remaining administrators.
   //for example, if there is only one ADMIN and that ADMIN is demoted, nobody will be able to manage roles anymore.
//What checks should we apply when demoting an ADMIN?
//I recommend the following business rules:
//Scenario	Expected result
//ADMIN promotes USER to ADMIN	Allow
//ADMIN demotes USER to USER	Allow (no-op)
//ADMIN demotes another ADMIN, with 2 or more admins existing	Allow
//ADMIN demotes another ADMIN, when they are the only ADMIN	Reject
//ADMIN demotes themselves, when another ADMIN exists	Allow
//ADMIN demotes themselves, when they are the only ADMIN	Reject
//ADMIN changes a user's role to their existing role	No change
//
//The key business rule is: Never allow the number of active ADMIN users to become zero.
//
//This is better than having separate rules for self-demotion and demoting another administrator.
//Example
//Suppose your database contains:
//User ID	Current role
//1	ADMIN
//2	ADMIN
//3	USER
//
//If ADMIN 1 demotes ADMIN 2:
//- Before the operation: 2 ADMIN users.
//- After the operation: 1 ADMIN user.
//- Result: Allowed.
//But if ADMIN 1 then tries to demote themselves:
//- Before the operation: 1 ADMIN user.
//- After the operation: 0 ADMIN users.
//- Result: Rejected.
//This is the safeguard I recommend.
    }
}
/*
Important production consideration: concurrent requests
Imagine there are two ADMIN users.

At almost the same time:
- ADMIN 1 tries to demote ADMIN 2.
- ADMIN 2 tries to demote ADMIN 1.
Both transactions could potentially read adminCount = 2.

Both pass the check and both demote their targets.

Result: zero ADMIN users.

Your @Transactional annotation alone does not prevent this race condition.

For your current learning project, the count check is a good first implementation. Before calling this production-grade, we'll need to protect this operation using database locking or another serialization strategy.

Test	Expected result
ADMIN promotes USER to ADMIN	Success
ADMIN demotes another ADMIN when 2 admins exist -->	Success
ADMIN demotes themselves when 2 admins exist-->	Success
Last ADMIN tries to demote themselves--> Reject
Last ADMIN tries to demote another ADMIN (if somehow the target is the only ADMIN)-->	Reject
ADMIN assigns ADMIN to an existing ADMIN	--> No change
ADMIN assigns USER to an existing USER	-->No change
Requested user ID doesn't exist -->	404

Also test that a normal USER receives 403 when calling the endpoint.

Next, let's write the unit tests for updateUserRole() using JUnit and Mockito.
We'll use your preferred simple Arrange → Act → Assert → Verify approach, without ArgumentCaptor.

One additional real-world consideration
In larger enterprise applications, demoting another administrator may require additional controls:
- Audit logging of who performed the change.
- Recording the previous and new roles.
- Notifications to affected administrators.
- Sometimes approval from another administrator for sensitive role changes.
For your project, audit logging is a good next enhancement, but a second-person approval workflow would be unnecessary complexity right now.

My recommendation: Keep your existing business rule. No change to your current count check is needed. Next, let's test all these scenarios with JUnit and Mockito.
 */

