package com.cartzilla.user.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class EmailVerificationToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public static EmailVerificationToken create(UUID userId, String token, Instant expiresAt) {
        if (userId == null) throw new BusinessException("userId must not be null");
        if (token == null || token.isBlank()) throw new BusinessException("token must not be blank");
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw new BusinessException("expiresAt must be in the future");
        }
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.userId = userId;
        verificationToken.token = token;
        verificationToken.expiresAt = expiresAt;
        return verificationToken;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed() {
        if (isUsed()) {
            throw new BusinessException("Email verification token already used");
        }
        this.usedAt = Instant.now();
        this.softDelete();
    }
}
