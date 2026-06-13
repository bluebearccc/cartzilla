package com.cartzilla.user.infrastructure.oauth;

import com.cartzilla.user.application.usecase.OAuthProfile;
import com.cartzilla.user.application.usecase.OAuthProviderGateway;
import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class HttpOAuthProviderGateway implements OAuthProviderGateway {
    private final OAuthProviderProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String buildAuthorizationUrl(OAuthProvider provider, String state) {
        OAuthProviderProperties.Registration registration = registration(provider);
        requireConfigured(registration.getAuthorizationUri(), "authorization-uri");
        requireConfigured(registration.getClientId(), "client-id");
        requireConfigured(registration.getRedirectUri(), "redirect-uri");

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(registration.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", registration.getClientId())
                .queryParam("redirect_uri", registration.getRedirectUri())
                .queryParam("state", state);
        if (registration.getScope() != null && !registration.getScope().isBlank()) {
            builder.queryParam("scope", registration.getScope());
        }
        return builder.build().toUriString();
    }

    @Override
    public OAuthProfile fetchProfile(OAuthProvider provider, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("OAuth authorization code is required");
        }
        OAuthProviderProperties.Registration registration = registration(provider);
        requireConfigured(registration.getTokenUri(), "token-uri");
        requireConfigured(registration.getUserInfoUri(), "user-info-uri");
        requireConfigured(registration.getClientId(), "client-id");
        requireConfigured(registration.getClientSecret(), "client-secret");
        requireConfigured(registration.getRedirectUri(), "redirect-uri");

        Map<?, ?> tokenResponse = exchangeCodeForToken(registration, code);
        Object accessToken = tokenResponse.get("access_token");
        if (accessToken == null || accessToken.toString().isBlank()) {
            throw new BusinessException("OAuth provider did not return access token");
        }
        Map<?, ?> userInfo = fetchUserInfo(registration, accessToken.toString());
        return toProfile(userInfo);
    }

    private Map<?, ?> exchangeCodeForToken(OAuthProviderProperties.Registration registration, String code) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", registration.getClientId());
        form.add("client_secret", registration.getClientSecret());
        form.add("redirect_uri", registration.getRedirectUri());

        return restClientBuilder.build().post()
                .uri(registration.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    private Map<?, ?> fetchUserInfo(OAuthProviderProperties.Registration registration, String accessToken) {
        return restClientBuilder.build().get()
                .uri(registration.getUserInfoUri())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);
    }

    private OAuthProfile toProfile(Map<?, ?> userInfo) {
        if (userInfo == null) {
            throw new BusinessException("OAuth provider did not return user profile");
        }
        return googleProfile(userInfo);
    }

    private OAuthProfile googleProfile(Map<?, ?> userInfo) {
        String id = value(userInfo, "sub");
        String email = value(userInfo, "email");
        String name = value(userInfo, "name");
        String picture = value(userInfo, "picture");
        Object verifiedValue = userInfo.get("email_verified");
        boolean verified = verifiedValue == null || Boolean.parseBoolean(verifiedValue.toString());
        return new OAuthProfile(id, email, name, picture, verified);
    }

    private OAuthProviderProperties.Registration registration(OAuthProvider provider) {
        OAuthProviderProperties.Registration registration =
                properties.getProviders().get(provider.name().toLowerCase());
        if (registration == null) {
            throw new BusinessException("OAuth provider is not configured: " + provider.name());
        }
        return registration;
    }

    private void requireConfigured(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("OAuth provider setting is missing: " + key);
        }
    }

    private String value(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }
}
