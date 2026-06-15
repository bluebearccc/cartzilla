package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.OAuthAccount;
import com.cartzilla.user.domain.vo.OAuthProvider;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository {
    OAuthAccount save(OAuthAccount account);
    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider);
}
