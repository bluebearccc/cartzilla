package com.cartzilla.notification.infrastructure.feign;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/** Cấu hình OpenFeign cho notification-service. */
@Configuration
public class FeignConfig {

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(5, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true);
    }

    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.BASIC;
    }
}
