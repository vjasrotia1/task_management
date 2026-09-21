package com.varun.taskmgmtapi.controller;

import com.varun.taskmgmtapi.dto.authDto.RegisterRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
//this class handles HTTP requests and returns response data, usually JSON
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //spring converts the JSON sent from postman into RegisterRequest registerRequest with the help of JACKSON
    //@Valid- tells spring to validate the REGISTER REQUEST dto using validation annotations
    //bec of @valid --> Spring performs validation before entering the service method.
    //if name,email,password are not as per RegisterRequest DTO, spring will create this exception(methodargnotvalidexception)
    // obj and throw MethodArgumentNotValidException, so add it to globalexception handler class

    @PostMapping("/register") //this annotation is bascially HTTP method
    @ResponseStatus(HttpStatus.CREATED) //201-client ko bina response body padhe smajh aa jana chahiye ki kya hua
    public UserResponse register(@Valid @RequestBody RegisterRequest registerRequest){

        return authService.register(registerRequest);
    }

    //now We'll build a proper global exception handler, so these errors return clean JSON.
}
