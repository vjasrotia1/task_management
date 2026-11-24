package com.varun.taskmgmtapi.controller;

import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRoleRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
//final --> This makes the dependency immutable after construction.
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/role/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUserRole(@PathVariable("userId") Long userId, @Valid @RequestBody UpdateUserRoleRequest updateUserRoleRequest) {
        //only admin can change the role of User
        User currentUser=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Long currentAdminId=currentUser.getId();
        //This lets us identify whether the administrator is changing their own role or someone else's role. It will also be useful if we later introduce audit logging
        //For now, the last-admin check works for both cases, but knowing the actor's ID is important for recording who made the change.

        return userService.updateUserRole(userId, updateUserRoleRequest,currentAdminId);
    }

}
/*
Recommended role-management rules
Action	Allowed?
USER changes their own role to ADMIN	No
USER changes another user's role	No
ADMIN promotes USER to ADMIN	Yes
ADMIN demotes another ADMIN to USER	Yes, with safeguards
ADMIN changes their own role to USER	Conditional

Should ADMIN be allowed to demote themselves?
Technically, yes. But consider this scenario:
- Your application has only one ADMIN (User 1).
- User 1 changes their own role from ADMIN to USER.
- Now there are zero ADMIN users.
- Nobody can promote another user or manage roles.
This can lock you out of administrative functionality.

My recommendation:
Allow an ADMIN to demote themselves only if at least one other active ADMIN exists.
 */
