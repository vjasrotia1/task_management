package com.varun.taskmgmtapi.controller;

import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRole;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/role/{userId}")
    public UserResponse updateUserRole(@PathVariable("userId") Long userId, @RequestBody UpdateUserRole updateUserRole) {

        return userService.UpdateUserRole(userId, updateUserRole);
    }

}
