package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.OAuthAccount;
import com.cartzilla.user.domain.repository.OAuthAccountRepository;
import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.user.infrastructure.persistence.OAuthAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuthAccountRepositoryAdapter implements OAuthAccountRepository {
    private final OAuthAccountJpaRepository jpa;

    @Override
    public OAuthAccount save(OAuthAccount account) {
        return jpa.save(account);
    }

    @Override
    public Optional<OAuthAccount> findByProviderAndProviderUserId(
            OAuthProvider provider, String providerUserId) {
        return jpa.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Override
    public Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider) {
        return jpa.findByUserIdAndProvider(userId, provider);
    }
}
