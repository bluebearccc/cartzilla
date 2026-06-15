package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.vo.OAuthProvider;

public interface OAuthProviderGateway {
    String buildAuthorizationUrl(OAuthProvider provider, String state);
    OAuthProfile fetchProfile(OAuthProvider provider, String code);
}
