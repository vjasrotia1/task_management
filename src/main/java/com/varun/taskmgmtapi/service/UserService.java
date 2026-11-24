package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRoleRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;

public interface UserService {
    UserResponse updateUserRole(Long userId, UpdateUserRoleRequest updateUserRoleRequest, Long CurrentAdminId);
}
