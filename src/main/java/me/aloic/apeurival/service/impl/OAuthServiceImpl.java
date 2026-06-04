package me.aloic.apeurival.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.converter.UserConverter;
import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.entity.mapper.UserMapper;
import me.aloic.apeurival.entity.mapper.UserOAuthMapper;
import me.aloic.apeurival.entity.po.UserOAuthPO;
import me.aloic.apeurival.entity.po.UserPO;
import me.aloic.apeurival.security.JwtUtils;
import me.aloic.apeurival.security.OAuthStateStore;
import me.aloic.apeurival.security.TokenEncryptor;
import me.aloic.apeurival.service.OAuthService;
import me.aloic.apeurival.service.oauth.OAuthProvider;
import me.aloic.apeurival.service.oauth.OAuthProvider.OAuthTokenResponse;
import me.aloic.apeurival.service.oauth.OAuthProvider.OAuthUserInfo;
import me.aloic.apeurival.service.oauth.OAuthProviderRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class OAuthServiceImpl implements OAuthService {

    private final OAuthProviderRegistry registry;
    private final OAuthStateStore stateStore;
    private final UserMapper userMapper;
    private final UserOAuthMapper userOAuthMapper;
    private final JwtUtils jwtUtils;
    private final TokenEncryptor tokenEncryptor;

    public OAuthServiceImpl(OAuthProviderRegistry registry,
                            OAuthStateStore stateStore,
                            UserMapper userMapper,
                            UserOAuthMapper userOAuthMapper,
                            TokenEncryptor tokenEncryptor,
                            JwtUtils jwtUtils) {
        this.registry = registry;
        this.stateStore = stateStore;
        this.userMapper = userMapper;
        this.userOAuthMapper = userOAuthMapper;
        this.jwtUtils = jwtUtils;
        this.tokenEncryptor=tokenEncryptor;
    }

    @Override
    public String getAuthorizationUrl(String provider) {
        OAuthProvider p = registry.get(provider);
        String state = stateStore.generate();
        return p.getAuthorizationUrl(state);
    }

    @Override
    @Transactional
    public String handleCallback(String provider, String code, String state) {
        if (!stateStore.validate(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired state");
        }

        OAuthProvider p = registry.get(provider);
        OAuthTokenResponse tokenResp = p.exchangeCode(code);
        OAuthUserInfo userInfo = p.getUserInfo(tokenResp.accessToken());

        // find existing binding
        UserOAuthPO binding = userOAuthMapper.selectOne(
                new QueryWrapper<UserOAuthPO>()
                        .eq("provider", provider)
                        .eq("provider_user_id", userInfo.providerUserId()));

        UserPO user;
        if (binding != null) {
            log.info("OAuth {} login: existing user id={}", provider, binding.getUserId());
            user = userMapper.selectById(binding.getUserId());
            updateTokens(binding, tokenResp);
        } else {
            log.info("OAuth {} login: auto-creating user for providerUserId={}", provider, userInfo.providerUserId());
            // auto-create account
            user = new UserPO();
            user.setUsername(userInfo.providerUsername());
            user.setDisplayName(userInfo.providerUsername());
            user.setAvatarUrl(userInfo.avatarUrl());
            user.setPasswordHash("");
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);

            binding = new UserOAuthPO();
            binding.setUserId(user.getId());
            binding.setProvider(provider);
            binding.setProviderUserId(userInfo.providerUserId());
            binding.setProviderUsername(userInfo.providerUsername());
            binding.setLinkedAt(LocalDateTime.now());
            fillTokens(binding, tokenResp);
            userOAuthMapper.insert(binding);
        }

        return jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public UserDTO linkAccount(Long userId, String provider, String code, String state) {
        if (!stateStore.validate(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired state");
        }

        OAuthProvider p = registry.get(provider);
        OAuthTokenResponse tokenResp = p.exchangeCode(code);
        OAuthUserInfo userInfo = p.getUserInfo(tokenResp.accessToken());

        // check if already bound to another user
        UserOAuthPO existing = userOAuthMapper.selectOne(
                new QueryWrapper<UserOAuthPO>()
                        .eq("provider", provider)
                        .eq("provider_user_id", userInfo.providerUserId()));
        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Already linked to your account");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This " + provider + " account is already linked to another user");
        }

        UserOAuthPO binding = new UserOAuthPO();
        binding.setUserId(userId);
        binding.setProvider(provider);
        binding.setProviderUserId(userInfo.providerUserId());
        binding.setProviderUsername(userInfo.providerUsername());
        binding.setLinkedAt(LocalDateTime.now());
        fillTokens(binding, tokenResp);
        userOAuthMapper.insert(binding);

        UserPO user = userMapper.selectById(userId);
        List<UserOAuthPO> links = userOAuthMapper.selectList(
                new QueryWrapper<UserOAuthPO>().eq("user_id", userId));
        return UserConverter.toDTO(user, links);
    }

    @Override
    public void unlinkAccount(Long userId, String provider) {
        UserOAuthPO binding = userOAuthMapper.selectOne(
                new QueryWrapper<UserOAuthPO>()
                        .eq("user_id", userId)
                        .eq("provider", provider));
        if (binding == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No linked " + provider + " account");
        }

        UserPO user = userMapper.selectById(userId);
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
        long oauthCount = userOAuthMapper.selectCount(
                new QueryWrapper<UserOAuthPO>().eq("user_id", userId));

        if (!hasPassword && oauthCount <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot unlink: set a password first, or this is your only login method");
        }

        userOAuthMapper.deleteById(binding.getId());
    }

    private void fillTokens(UserOAuthPO po, OAuthTokenResponse resp) {
        po.setAccessToken(tokenEncryptor.encrypt(resp.accessToken()));
        if (resp.refreshToken() != null) {
            po.setRefreshToken(tokenEncryptor.encrypt(resp.refreshToken()));
        }
        if (resp.expiresIn() > 0) {
            po.setTokenExpiresAt(LocalDateTime.ofInstant(
                    Instant.now().plusSeconds(resp.expiresIn()), ZoneId.systemDefault()));
        }
    }

    private void updateTokens(UserOAuthPO po, OAuthTokenResponse resp) {
        po.setAccessToken(resp.accessToken());
        if (resp.refreshToken() != null) {
            po.setRefreshToken(resp.refreshToken());
        }
        if (resp.expiresIn() > 0) {
            po.setTokenExpiresAt(LocalDateTime.ofInstant(
                    Instant.now().plusSeconds(resp.expiresIn()), ZoneId.systemDefault()));
        }
        userOAuthMapper.updateById(po);
    }
}
