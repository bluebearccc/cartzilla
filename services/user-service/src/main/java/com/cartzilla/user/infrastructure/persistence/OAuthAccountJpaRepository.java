package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.OAuthAccount;
import com.cartzilla.user.domain.vo.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccount, UUID> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);
    Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider);
}
