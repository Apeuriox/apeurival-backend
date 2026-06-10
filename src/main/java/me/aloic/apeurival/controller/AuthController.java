package me.aloic.apeurival.controller;

import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.entity.dto.LoginRequest;
import me.aloic.apeurival.entity.dto.RegisterRequest;
import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.security.JwtUtils;
import me.aloic.apeurival.security.OAuthStateStore;
import me.aloic.apeurival.service.OAuthService;
import me.aloic.apeurival.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final OAuthService oAuthService;
    private final OAuthStateStore stateStore;
    private final JwtUtils jwtUtils;
    private final String frontendUrl;

    public AuthController(UserService userService,
                          OAuthService oAuthService,
                          OAuthStateStore stateStore,
                          JwtUtils jwtUtils,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.userService = userService;
        this.oAuthService = oAuthService;
        this.stateStore = stateStore;
        this.jwtUtils = jwtUtils;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        log.info("[POST] handling new user register /api/auth/register target username： {}",request.getUsername());
        UserDTO user = userService.register(request);
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", token, "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("[POST] handling user login /api/auth/login target username： {}",request.getUsername());
        UserDTO user = userService.login(request);
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        return ResponseEntity.ok(Map.of("token", token, "user", user));
    }

    @GetMapping("/oauth/{provider}/url")
    public ResponseEntity<Map<String, String>> oauthUrl(@PathVariable String provider) {
        log.info("[GET] handling oauth /api/auth/oauth");
        String url = oAuthService.getAuthorizationUrl(provider);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        log.info("[GET] handling oauth callback /api/auth/oauth/callback");
        String token = oAuthService.handleCallback(provider, code, state);
        String authCode = stateStore.storeToken(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/oauth-callback?code=" + authCode))
                .build();
    }

    @PostMapping("/oauth/exchange")
    public ResponseEntity<Map<String, Object>> exchangeToken(@RequestParam String code) {
        log.info("[POST] handling oauth exchange /api/auth/oauth/exchange");
        String token = stateStore.retrieveToken(code);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired exchange code");
        }
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/oauth/{provider}/link")
    public ResponseEntity<Map<String, Object>> linkOAuth(
            @PathVariable String provider,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        log.info("[POST] handling oauth link /api/auth/oauth/link");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        UserDTO user = oAuthService.linkAccount(userId, provider,
                body.get("code"), body.get("state"));
        return ResponseEntity.ok(Map.of("linked", true, "user", user));
    }

    @DeleteMapping("/oauth/{provider}")
    public ResponseEntity<Map<String, Object>> unlinkOAuth(
            @PathVariable String provider,
            Authentication auth) {
        log.info("[DELETE] handling oauth unlink /api/auth/oauth");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        oAuthService.unlinkAccount(userId, provider);
        return ResponseEntity.ok(Map.of("unlinked", true));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(Authentication auth) {
        log.info("[GET] handling get myself /api/auth/me");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        log.info("[PUT] handling password change /api/auth/password");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        userService.changePassword(userId,
                body.get("oldPassword"),
                body.get("newPassword"));
        return ResponseEntity.ok(Map.of("changed", true));
    }

    @PutMapping("/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        log.info("[GET] handling avatar change /api/auth/avatar");
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        UserDTO user = userService.updateAvatar(userId, body.get("avatarUrl"));
        return ResponseEntity.ok(Map.of("avatarUrl", user.getAvatarUrl()));
    }
}
