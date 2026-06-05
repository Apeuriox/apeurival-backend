package me.aloic.apeurival.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccessModeFilter extends OncePerRequestFilter {

    private final boolean restricted;

    public AccessModeFilter(@Value("${app.auth.access-mode}") String accessMode) {
        this.restricted = "RESTRICTED".equalsIgnoreCase(accessMode);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!restricted) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // always allow auth endpoints
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String role = ga.getAuthority();
                if ("ROLE_EDITOR".equals(role) || "ROLE_ADMIN".equals(role)) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }

        response.setStatus(403);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Access denied — site is restricted\"}");
    }
}
