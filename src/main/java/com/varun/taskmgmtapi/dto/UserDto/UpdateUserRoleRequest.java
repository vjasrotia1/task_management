package com.varun.taskmgmtapi.dto.UserDto;

import com.varun.taskmgmtapi.models.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {
    @NotNull(message = "Role cannot be null")

    private Role role;
}
