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
//Spring/Jackson tries to convert the JSON request into your UpdateUserRoleRequest object.
//Because superadmin is not a valid enum value, JSON deserialization fails before the controller method execute
//An invalid enum value generally produces HTTP 400 during JSON deserialization.