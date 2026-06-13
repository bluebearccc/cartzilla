package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    List<PasswordResetToken> findByUserId(UUID userId);
}
