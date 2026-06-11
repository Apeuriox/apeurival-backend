package me.aloic.apeurival.service;

public interface OAuthProvider {

    String getProviderName();

    String getAuthorizationUrl(String state);

    OAuthTokenResponse exchangeCode(String code);

    OAuthUserInfo getUserInfo(String accessToken);

    record OAuthTokenResponse(String accessToken, String refreshToken, long expiresIn) {}

    record OAuthUserInfo(String providerUserId, String providerUsername, String avatarUrl) {}
}
