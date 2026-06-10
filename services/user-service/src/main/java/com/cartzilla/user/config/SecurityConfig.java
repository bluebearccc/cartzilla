package com.cartzilla.user.config;

import com.cartzilla.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.Optional;

@Configuration
@Import(JwtTokenProvider.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** AuditorAware lấy user hiện tại cho BaseEntity.createdBy/updatedBy. */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("system"); // TODO: lấy từ header X-User-Id (gateway inject)
    }

    /** Service nằm sau gateway → tắt security nội bộ, gateway đã validate JWT. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}
