package com.cartzilla.user.infrastructure.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "oauth")
public class OAuthProviderProperties {
    private Map<String, Registration> providers = new HashMap<>();

    @Getter
    @Setter
    public static class Registration {
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String redirectUri;
        private String scope;
    }
}
