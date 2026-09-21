package com.varun.taskmgmtapi.ServiceImpl;

import com.varun.taskmgmtapi.dto.authDto.RegisterRequest;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.exception.ResourceAlreadyExistsException;
import com.varun.taskmgmtapi.models.Role;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.UserRepo;
import com.varun.taskmgmtapi.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service //tells spring that : this is a service component and creat and manage an obj of this class
//we will inject obj of this class in our controller
public class AuthServiceImpl implements AuthService {

    private UserRepo userRepo;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    //constructor injection
    //before this, we need to create a bean of datatype BCryptPasswordEncoder
    public AuthServiceImpl(UserRepo userRepo, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepo = userRepo;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }


    @Override
    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {

        if(userRepo.existsByEmail(registerRequest.getEmail())) {
            throw new ResourceAlreadyExistsException("email already exists");
        }

        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(bCryptPasswordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.USER);

        userRepo.save(user);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
