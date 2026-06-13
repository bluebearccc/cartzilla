package com.cartzilla.user.application.usecase;

import com.cartzilla.security.JwtTokenProvider;
import com.cartzilla.user.domain.entity.OAuthAccount;
import com.cartzilla.user.domain.entity.RefreshToken;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.OAuthAccountRepository;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CompleteOAuthLoginUseCase {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthProviderGateway oauthProviderGateway;
    private final OAuthStateStore oauthStateStore;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenGenerator tokenGenerator;

    @Value("${jwt.refresh-ttl-ms:604800000}")
    private long refreshTtlMs;

    public record Result(String accessToken, String refreshToken, String email, String role) {}

    @Transactional
    public Result execute(OAuthProvider provider, String code, String state) {
        oauthStateStore.validateAndConsume(provider, state);
        OAuthProfile profile = oauthProviderGateway.fetchProfile(provider, code);
        validateProfile(profile);

        OAuthAccount account = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .orElse(null);
        User user;
        if (account != null) {
            user = userRepository.findById(account.getUserId())
                    .orElseThrow(() -> new BusinessException("Linked user not found"));
        } else {
            user = findOrCreateUser(provider, profile);
            account = OAuthAccount.link(
                    user.getId(), provider, profile.providerUserId(), profile.email(),
                    profile.displayName(), profile.avatarUrl());
        }

        user.requireActive();
        account.recordLogin();
        oauthAccountRepository.save(account);
        return issueTokens(user);
    }

    private User findOrCreateUser(OAuthProvider provider, OAuthProfile profile) {
        return userRepository.findByEmail(profile.email())
                .map(existing -> {
                    existing.requireActive();
                    oauthAccountRepository.findByUserIdAndProvider(existing.getId(), provider)
                            .ifPresent(account -> {
                                throw new BusinessException("OAuth provider already linked to this user");
                            });
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.createOAuthUser(
                        profile.email(), profile.displayName(), Role.CUSTOMER)));
    }

    private void validateProfile(OAuthProfile profile) {
        if (profile == null) {
            throw new BusinessException("OAuth profile is missing");
        }
        if (profile.providerUserId() == null || profile.providerUserId().isBlank()) {
            throw new BusinessException("OAuth provider user id is missing");
        }
        if (profile.email() == null || profile.email().isBlank()) {
            throw new BusinessException("OAuth email is missing");
        }
        if (!profile.emailVerified()) {
            throw new BusinessException("OAuth email is not verified");
        }
    }

    private Result issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenGenerator.generateUrlSafeToken();
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(), refreshToken, Instant.now().plus(Duration.ofMillis(refreshTtlMs))));
        return new Result(accessToken, refreshToken, user.getEmail(), user.getRole().name());
    }
}
