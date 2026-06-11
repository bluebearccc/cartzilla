package com.cartzilla.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * scanBasePackages gồm cả com.cartzilla.security để pick-up bean JwtTokenProvider
 * (nằm trong module shared/common-security) cho JwtAuthFilter inject được.
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.cartzilla.gateway", "com.cartzilla.security"})
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
