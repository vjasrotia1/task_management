package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.authDto.LoginRequest;
import com.varun.taskmgmtapi.dto.authDto.LoginResponse;
import com.varun.taskmgmtapi.dto.authDto.RegisterRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest registerRequest);
    //loginrequest aane par, loginresponse return krna hai client ko
    LoginResponse login(LoginRequest loginRequest);

}
