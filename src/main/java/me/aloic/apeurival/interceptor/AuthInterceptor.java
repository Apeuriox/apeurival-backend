package me.aloic.apeurival.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import me.aloic.apeurival.annotation.RequireAuth;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final String apiKey;

    public AuthInterceptor(@Value("${app.auth.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        RequireAuth annotation = hm.getMethodAnnotation(RequireAuth.class);
        if (annotation == null) {
            return true;
        }

        String header = request.getHeader("X-Api-Key");
        if (apiKey.equals(header)) {
            return true;
        }

        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
        return false;
    }
}
