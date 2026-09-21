package com.varun.taskmgmtapi.dto.authDto;

//this class is the response we want to give back to the User
import com.varun.taskmgmtapi.models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
}
