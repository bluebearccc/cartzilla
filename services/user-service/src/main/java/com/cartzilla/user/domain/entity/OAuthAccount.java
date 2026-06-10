package com.cartzilla.user.domain.entity;

import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/**
 * Child entity của UserAggregate.
 * Rules: OA-01..OA-05.
 * UNIQUE (provider, providerUserId) — OA-02
 * UNIQUE (userId, provider)         — OA-03
 */
@Entity
@Table(name = "oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_oauth_provider_user", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uq_oauth_user_provider", columnNames = {"user_id", "provider"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class OAuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** OA-01: GOOGLE | FACEBOOK */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /** OA-04: NOT NULL, default NOW */
    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    /** OA-05: lastLoginAt ≥ linkedAt khi update */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Factory — OA-04 */
    public static OAuthAccount link(UUID userId, OAuthProvider provider,
                                     String providerUserId, String providerEmail,
                                     String displayName, String avatarUrl) {
        if (userId == null) throw new BusinessException("userId must not be null");
        if (provider == null) throw new BusinessException("provider must not be null (OA-01)");
        if (providerUserId == null || providerUserId.isBlank())
            throw new BusinessException("providerUserId must not be blank");
        OAuthAccount oa = new OAuthAccount();
        oa.userId = userId;
        oa.provider = provider;
        oa.providerUserId = providerUserId;
        oa.providerEmail = providerEmail;
        oa.displayName = displayName;
        oa.avatarUrl = avatarUrl;
        oa.linkedAt = Instant.now();
        return oa;
    }

    /** OA-05: lastLoginAt ≥ linkedAt */
    public void recordLogin() {
        Instant now = Instant.now();
        if (now.isBefore(linkedAt))
            throw new BusinessException("lastLoginAt cannot be before linkedAt (OA-05)");
        this.lastLoginAt = now;
    }
}
