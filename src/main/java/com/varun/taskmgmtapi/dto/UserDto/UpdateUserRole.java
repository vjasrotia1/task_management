package com.varun.taskmgmtapi.dto.UserDto;

import com.varun.taskmgmtapi.models.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRole {
    private Role role;
}
