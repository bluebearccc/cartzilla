package com.cartzilla.user.domain.entity;

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
 * Rules: RT-01..RT-04.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** RT-01: unique, NOT NULL */
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    /** RT-02: NOT NULL, phải > now khi tạo */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Factory — RT-02: expiresAt > now */
    public static RefreshToken create(UUID userId, String token, Instant expiresAt) {
        if (userId == null) throw new BusinessException("userId must not be null");
        if (token == null || token.isBlank()) throw new BusinessException("token must not be blank");
        if (expiresAt == null || !expiresAt.isAfter(Instant.now()))
            throw new BusinessException("expiresAt must be in the future (RT-02)");
        RefreshToken rt = new RefreshToken();
        rt.userId = userId;
        rt.token = token;
        rt.expiresAt = expiresAt;
        return rt;
    }

    /** RT-03: kiểm tra hết hạn */
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }

    /** RT-03: soft-revoke */
    public void revoke() { this.softDelete(); }
}
