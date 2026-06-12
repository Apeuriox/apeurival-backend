package me.aloic.apeurival.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.config.UploadDirInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Component
public class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtils(@Value("${app.auth.jwt-secret}") String secret,
                    @Value("${app.auth.jwt-expiration-days}") long days) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        this.expirationMs = days * 24 * 60 * 60 * 1000L;
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        log.info("Token issuing for {}, date: {}", username, now);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        Claims parsedToken = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        log.info("Token parsed for {}, Issued at: {}", parsedToken.getIssuer(), parsedToken.getIssuedAt());
        return parsedToken;
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }
}
