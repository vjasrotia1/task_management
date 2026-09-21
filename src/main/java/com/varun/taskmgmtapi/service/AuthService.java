package com.varun.taskmgmtapi.service;

import com.varun.taskmgmtapi.dto.authDto.RegisterRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest registerRequest);

}
