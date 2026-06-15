package com.cartzilla.user.infrastructure.oauth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OAuthProviderProperties.class)
public class OAuthInfrastructureConfig {
}
