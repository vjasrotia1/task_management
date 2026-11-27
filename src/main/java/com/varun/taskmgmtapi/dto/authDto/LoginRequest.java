package com.varun.taskmgmtapi.dto.authDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "email is required")
    @Email(message="please provide valid email")
    private String email;

    @NotBlank(message = "password is required")
    private String password;
    //One important point: Don't add @Size(min=8) to the login password.
    //Why? If a user registered with an existing password and later the password policy changes,
    // login should still allow them to authenticate. Registration enforces the password policy; login verifies the existing credentials.
}
