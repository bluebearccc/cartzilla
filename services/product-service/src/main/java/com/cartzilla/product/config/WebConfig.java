package com.cartzilla.product.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;

/**
 * Defense-in-depth cho permission matrix (SRS §2.3):
 * gateway validate JWT + inject X-User-Role; service tự enforce ADMIN cho /api/admin/**.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminRoleInterceptor())
                .addPathPatterns("/api/admin/**");
    }

    static class AdminRoleInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) throws Exception {
            if ("ADMIN".equals(request.getHeader("X-User-Role"))) {
                return true;
            }
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            // cùng shape với ApiResponse
            response.getWriter().write("{\"success\":false,\"message\":\"ADMIN role required\","
                    + "\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}");
            return false;
        }
    }
}
