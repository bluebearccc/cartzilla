package com.cartzilla.order.infrastructure.feign;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình OpenFeign dùng chung cho order-service.
 * - Truyền JWT token xuống các service downstream.
 * - Timeout connect: 5s, read: 10s.
 * - Log level FULL ở dev.
 */
@Configuration
public class FeignConfig {

    /** Truyền Authorization header từ request hiện tại xuống Feign call */
    @Bean
    public RequestInterceptor jwtForwardInterceptor() {
        return template -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                String auth = servletAttrs.getRequest().getHeader("Authorization");
                if (auth != null && !auth.isBlank()) {
                    template.header("Authorization", auth);
                }
            }
        };
    }

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(5, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true);
    }

    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }
}
