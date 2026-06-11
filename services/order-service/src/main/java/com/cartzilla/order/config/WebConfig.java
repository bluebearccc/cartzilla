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
        registry.addInterceptor(new StaffRoleInterceptor())
                .addPathPatterns("/api/staff/**");
    }

    static class StaffRoleInterceptor implements HandlerInterceptor {
        private static final Set<String> ALLOWED = Set.of("STAFF", "ADMIN");

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            if (ALLOWED.contains(request.getHeader("X-User-Role"))) {
                return true;
            }
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"STAFF or ADMIN role required\","
                    + "\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}");
            return false;
        }
    }
}
