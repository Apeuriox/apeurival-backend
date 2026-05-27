package me.aloic.apeurival.service;

import me.aloic.apeurival.entity.dto.LoginRequest;
import me.aloic.apeurival.entity.dto.RegisterRequest;
import me.aloic.apeurival.entity.dto.UserDTO;

public interface UserService {

    UserDTO register(RegisterRequest request);

    UserDTO login(LoginRequest request);

    UserDTO getCurrentUser(Long userId);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
