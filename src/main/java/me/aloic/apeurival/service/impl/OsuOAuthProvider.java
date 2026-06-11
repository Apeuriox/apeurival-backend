package me.aloic.apeurival.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.service.OAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class OsuOAuthProvider implements OAuthProvider
{

    private static final String PROVIDER_NAME = "osu";
    private static final String AUTHORIZE_URL = "https://osu.ppy.sh/oauth/authorize";
    private static final String TOKEN_URL = "https://osu.ppy.sh/oauth/token";
    private static final String USER_INFO_URL = "https://osu.ppy.sh/api/v2/me";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestTemplate restTemplate = new RestTemplate();

    public OsuOAuthProvider(
            @Value("${app.oauth.osu.client-id}") String clientId,
            @Value("${app.oauth.osu.client-secret}") String clientSecret,
            @Value("${app.oauth.osu.redirect-uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getAuthorizationUrl(String state) {
        return AUTHORIZE_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=identify"
                + "&state=" + state;
    }

    @Override
    public OAuthTokenResponse exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", redirectUri);

        ResponseEntity<Map> resp = restTemplate.exchange(
                TOKEN_URL,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = resp.getBody();
        log.info("osu! token exchange OK, expires_in={}", data.get("expires_in"));
        Number expiresIn = (Number) data.get("expires_in");
        return new OAuthTokenResponse(
                (String) data.get("access_token"),
                (String) data.get("refresh_token"),
                expiresIn != null ? expiresIn.longValue() : 0);
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> resp = restTemplate.exchange(
                USER_INFO_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = resp.getBody();
        log.info("osu! user info OK: id={}, username={}", data.get("id"), data.get("username"));
        return new OAuthUserInfo(
                String.valueOf(data.get("id")),
                (String) data.get("username"),
                (String) data.get("avatar_url"));
    }
}
