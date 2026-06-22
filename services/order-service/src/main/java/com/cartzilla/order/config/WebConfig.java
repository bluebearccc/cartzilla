package com.cartzilla.order.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.util.Set;

/**
 * Defense-in-depth cho permission matrix (SRS §2.3):
 * gateway validate JWT + inject X-User-Role; service tự enforce STAFF/ADMIN cho /api/staff/**.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleInterceptor(Set.of("STAFF", "ADMIN"), "STAFF or ADMIN role required"))
                .addPathPatterns("/api/staff/**");
        registry.addInterceptor(new RoleInterceptor(Set.of("ADMIN"), "ADMIN role required"))
                .addPathPatterns("/api/admin/**");
    }

    /** Defense-in-depth: kiểm tra X-User-Role do gateway inject. */
    static class RoleInterceptor implements HandlerInterceptor {
        private final Set<String> allowed;
        private final String message;

        RoleInterceptor(Set<String> allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            if (allowed.contains(request.getHeader("X-User-Role"))) {
                return true;
            }
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\","
                    + "\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}");
            return false;
        }
    }
}
