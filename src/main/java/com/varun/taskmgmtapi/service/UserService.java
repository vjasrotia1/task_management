package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRole;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.models.User;

public interface UserService {
    UserResponse UpdateUserRole(Long userId, UpdateUserRole  updateUserRole);
}
