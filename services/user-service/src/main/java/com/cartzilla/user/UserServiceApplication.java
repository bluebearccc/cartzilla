package com.cartzilla.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * scanBasePackages gồm cả com.cartzilla.security để pick-up JwtTokenProvider
 * (module shared/common-security) cho LoginUseCase / RefreshToken inject được.
 */
@EnableJpaAuditing
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.cartzilla.user", "com.cartzilla.security"})
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
