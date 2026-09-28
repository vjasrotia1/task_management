package com.varun.taskmgmtapi.dto.authDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "email is required")
    @Email(message="please provide valid email")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "enter correct password")
    private String password;
}
