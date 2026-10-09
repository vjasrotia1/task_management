package com.varun.taskmgmtapi.ServiceImpl;

import com.varun.taskmgmtapi.dto.UserDto.UpdateUserRole;
import com.varun.taskmgmtapi.dto.authDto.UserResponse;
import com.varun.taskmgmtapi.exception.UserNotFoundException;
import com.varun.taskmgmtapi.models.User;
import com.varun.taskmgmtapi.repository.UserRepo;
import com.varun.taskmgmtapi.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private UserRepo userRepo;
    public UserServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    @Transactional
    public UserResponse UpdateUserRole(Long userId, UpdateUserRole updateUserRole) {
        User user=userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

        user.setRole(updateUserRole.getRole());
        userRepo.save(user);
        return convertUserToUserResponse(user);
    }

    private UserResponse convertUserToUserResponse(User user){
        UserResponse userResponse=new UserResponse(
                user.getId(), user.getName(),user.getEmail(),user.getRole()
        );
        return userResponse;
    }
}
