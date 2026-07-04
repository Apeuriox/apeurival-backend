package me.aloic.apeurival.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.aloic.apeurival.enums.RoleEnum;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final TokenInvalidationStore invalidationStore;

    public JwtAuthFilter(JwtUtils jwtUtils, TokenInvalidationStore invalidationStore) {
        this.jwtUtils = jwtUtils;
        this.invalidationStore = invalidationStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            Claims claims = jwtUtils.parseToken(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String roleStr = claims.get("role", String.class);

            RoleEnum role;
            try {
                role = RoleEnum.fromString(roleStr);
            } catch (IllegalArgumentException e) {
                log.warn("JWT contains unknown role '{}', rejecting", roleStr);
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid role in token\"}");
                return;
            }
            if (role == null) {
                log.warn("JWT missing role claim, rejecting");
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing role in token\"}");
                return;
            }

            Long userIdLong = Long.valueOf(userId);
            Long iat = claims.get("iat", Long.class);
            if (iat != null) {
                if (!invalidationStore.isTokenValid(userIdLong, iat)) {
                    log.warn("Token invalidated for user {}, issued at {}", userId, iat);
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token has been invalidated\"}");
                    return;
                }
            } else {
                log.debug("Token missing iat claim for user {} (legacy token, skipping invalidation check)", userId);
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority(role.toAuthority().getAuthority())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
