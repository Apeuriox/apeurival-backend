package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.util.Collections;
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

        return toDTO(po);
    }

    @Override
    public UserDTO login(LoginRequest req) {
        UserPO po = userMapper.selectOne(
                new QueryWrapper<UserPO>().eq("username", req.getUsername()));
        if (po == null || !passwordEncoder.matches(req.getPassword(), po.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password provided");
        }
        return toDTO(po);
    }

    @Override
    public UserDTO getCurrentUser(Long userId) {
        UserPO po = userMapper.selectById(userId);
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return toDTO(po);
    }

    UserDTO toDTO(UserPO po) {
        UserDTO dto = new UserDTO();
        dto.setId(po.getId());
        dto.setUsername(po.getUsername());
        dto.setEmail(po.getEmail());
        dto.setDisplayName(po.getDisplayName());
        dto.setAvatarUrl(po.getAvatarUrl());
        dto.setRole(po.getRole());
        dto.setCreatedAt(po.getCreatedAt());

        List<UserOAuthPO> links = userOAuthMapper.selectList(
                new QueryWrapper<UserOAuthPO>().eq("user_id", po.getId()));
        if (!links.isEmpty()) {
            dto.setLinkedAccounts(links.stream().map(l -> {
                UserDTO.LinkedAccount la = new UserDTO.LinkedAccount();
                la.setProvider(l.getProvider());
                la.setProviderUsername(l.getProviderUsername());
                la.setLinkedAt(l.getLinkedAt());
                return la;
            }).toList());
        } else {
            dto.setLinkedAccounts(Collections.emptyList());
        }
        return dto;
    }
}
