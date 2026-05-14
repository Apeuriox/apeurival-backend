package me.aloic.apeurival.service;

import me.aloic.apeurival.entity.dto.UserDTO;

public interface OAuthService {

    String getAuthorizationUrl(String provider);

    String handleCallback(String provider, String code, String state);

    UserDTO linkAccount(Long userId, String provider, String code, String state);

    void unlinkAccount(Long userId, String provider);
}
