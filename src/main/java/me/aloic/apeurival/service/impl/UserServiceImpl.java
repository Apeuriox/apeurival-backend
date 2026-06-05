package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.aloic.apeurival.converter.UserConverter;
import me.aloic.apeurival.entity.dto.LoginRequest;
import me.aloic.apeurival.entity.dto.RegisterRequest;
import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.UserOAuthMapper;
import me.aloic.apeurival.entity.po.UserOAuthPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserOAuthMapper userOAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean isRegistrationOpen;

    public UserServiceImpl(UserMapper userMapper,
                           UserOAuthMapper userOAuthMapper,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.auth.registration-open}") boolean isRegistrationOpen) {
        this.userMapper = userMapper;
        this.userOAuthMapper = userOAuthMapper;
        this.passwordEncoder = passwordEncoder;
        this.isRegistrationOpen = isRegistrationOpen;
    }

    @Override
    public UserDTO register(RegisterRequest req) {
        if (!isRegistrationOpen) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration temporarily closed");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        UserPO existing = userMapper.selectOne(
                new QueryWrapper<UserPO>().eq("username", req.getUsername()));
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserPO po = new UserPO();
        po.setUsername(req.getUsername());
        po.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        po.setEmail(req.getEmail());
        po.setDisplayName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername());
        po.setRole("USER");
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(po);

        return UserConverter.toDTO(po, List.of());
    }

    @Override
    public UserDTO login(LoginRequest req) {
        UserPO po = userMapper.selectOne(
                new QueryWrapper<UserPO>().eq("username", req.getUsername()));
        if (po == null || !passwordEncoder.matches(req.getPassword(), po.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password provided");
        }
        return UserConverter.toDTO(po, linkedAccounts(po.getId()));
    }

    @Override
    public UserDTO getCurrentUser(Long userId) {
        UserPO po = userMapper.selectById(userId);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return UserConverter.toDTO(po, linkedAccounts(userId));
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        UserPO po = userMapper.selectById(userId);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        boolean hasPassword = po.getPasswordHash() != null && !po.getPasswordHash().isBlank();
        if (hasPassword && !passwordEncoder.matches(oldPassword, po.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Old password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }
        po.setPasswordHash(passwordEncoder.encode(newPassword));
        po.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(po);
    }

    @Override
    public UserDTO updateAvatar(Long userId, String avatarUrl) {
        UserPO po = userMapper.selectById(userId);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        po.setAvatarUrl(avatarUrl);
        po.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(po);
        return UserConverter.toDTO(po, linkedAccounts(userId));
    }

    private List<UserOAuthPO> linkedAccounts(Long userId) {
        return userOAuthMapper.selectList(
                new QueryWrapper<UserOAuthPO>().eq("user_id", userId));
    }
}
