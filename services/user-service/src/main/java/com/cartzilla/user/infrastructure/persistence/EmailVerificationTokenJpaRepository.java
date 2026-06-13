package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);
    List<EmailVerificationToken> findByUserId(UUID userId);
}
