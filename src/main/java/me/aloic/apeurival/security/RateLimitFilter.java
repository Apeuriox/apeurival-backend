package me.aloic.apeurival.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 300;  // 5 minutes
    private static final long LOCKOUT_SECONDS = 900; // 15 minutes

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (!requiresRateLimit(uri, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Instant now = Instant.now();
        AttemptWindow window = attempts.computeIfAbsent(ip, k -> new AttemptWindow());

        synchronized (window) {
            if (window.lockedUntil != null) {
                if (now.isBefore(window.lockedUntil)) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Too many attempts. Try again later.\"}");
                    return;
                }
                window.lockedUntil = null;
                window.count = 0;
                window.windowStart = now;
            }

            if (window.windowStart == null || now.isAfter(window.windowStart.plusSeconds(WINDOW_SECONDS))) {
                window.count = 0;
                window.windowStart = now;
            }

            window.count++;
            if (window.count > MAX_ATTEMPTS) {
                window.lockedUntil = now.plusSeconds(LOCKOUT_SECONDS);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many attempts. Account locked for 15 minutes.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresRateLimit(String uri, String method) {
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) return false;

        // login: 10 attempts / 5 min → 15 min lock
        if ("/api/auth/login".equals(uri)) return true;

        // register: 5 attempts / 5 min → 15 min lock
        if ("/api/auth/register".equals(uri)) return true;

        // change password: 5 attempts / 5 min → 15 min lock
        if ("/api/auth/password".equals(uri)) return true;

        // image upload: 30 per 5 min → 5 min lock
        if ("/api/upload/image".equals(uri)) return true;

        return false;
    }

    private static class AttemptWindow {
        int count;
        Instant windowStart;
        Instant lockedUntil;
    }
}
