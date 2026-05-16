package me.aloic.apeurival.controller;

import me.aloic.apeurival.entity.dto.LoginRequest;
import me.aloic.apeurival.entity.dto.RegisterRequest;
import me.aloic.apeurival.entity.dto.UserDTO;
import me.aloic.apeurival.security.JwtUtils;
import me.aloic.apeurival.service.OAuthService;
import me.aloic.apeurival.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final OAuthService oAuthService;
    private final JwtUtils jwtUtils;
    private final String frontendUrl;

    public AuthController(UserService userService,
                          OAuthService oAuthService,
                          JwtUtils jwtUtils,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.userService = userService;
        this.oAuthService = oAuthService;
        this.jwtUtils = jwtUtils;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        UserDTO user = userService.register(request);
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", token, "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        UserDTO user = userService.login(request);
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        return ResponseEntity.ok(Map.of("token", token, "user", user));
    }

    @GetMapping("/oauth/{provider}/url")
    public ResponseEntity<Map<String, String>> oauthUrl(@PathVariable String provider) {
        String url = oAuthService.getAuthorizationUrl(provider);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state) {
        String token = oAuthService.handleCallback(provider, code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/oauth-callback?token=" + token))
                .build();
    }

    @PostMapping("/oauth/{provider}/link")
    public ResponseEntity<Map<String, Object>> linkOAuth(
            @PathVariable String provider,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        UserDTO user = oAuthService.linkAccount(userId, provider,
                body.get("code"), body.get("state"));
        return ResponseEntity.ok(Map.of("linked", true, "user", user));
    }

    @DeleteMapping("/oauth/{provider}")
    public ResponseEntity<Map<String, Object>> unlinkOAuth(
            @PathVariable String provider,
            Authentication auth) {
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        oAuthService.unlinkAccount(userId, provider);
        return ResponseEntity.ok(Map.of("unlinked", true));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(Authentication auth) {
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }
}
