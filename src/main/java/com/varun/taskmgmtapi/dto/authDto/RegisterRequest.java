package com.varun.taskmgmtapi.dto.authDto;

//user wants to register
//this is out user creation request dto
//this DTO controls exactly what the API accepts.
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message="please provide a valid email")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min=8,message="password must contain atleast 8 characters")
    private String password;


    //we cant do this
    /*
    @PostMapping
public User register(@RequestBody User user)

i.e. we cant receive user from client/postman/swagger
bec, user is our db entity and we dont want to expose it to client
     */

}
