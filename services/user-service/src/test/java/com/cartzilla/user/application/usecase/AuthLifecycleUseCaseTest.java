package com.cartzilla.user.application.usecase;

import com.cartzilla.security.JwtTokenProvider;
import com.cartzilla.user.application.command.AuthCommand;
import com.cartzilla.user.domain.entity.EmailVerificationToken;
import com.cartzilla.user.domain.entity.PasswordResetToken;
import com.cartzilla.user.domain.entity.RefreshToken;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.EmailVerificationTokenRepository;
import com.cartzilla.user.domain.repository.PasswordResetTokenRepository;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.UserSearchCriteria;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.user.infrastructure.feign.NotificationFeignClient;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthLifecycleUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
    private final InMemoryPasswordResetTokenRepository resetTokenRepository = new InMemoryPasswordResetTokenRepository();
    private final InMemoryEmailVerificationTokenRepository verificationTokenRepository =
            new InMemoryEmailVerificationTokenRepository();
    private final PasswordEncoder passwordEncoder = new PrefixPasswordEncoder();
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            "dev-secret-key-change-me-please-32bytes!!", 900000);
    private final StubTokenGenerator tokenGenerator = new StubTokenGenerator();
    private final FakeNotificationFeignClient notificationFeignClient = new FakeNotificationFeignClient();

    // ───────────────────────── Login ─────────────────────────

    @Test
    void loginIssuesRefreshTokenAndRefreshRotatesIt() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:secret123", "Buyer"));
        user.verifyEmail();
        LoginUseCase loginUseCase = loginUseCase();
        RefreshTokenUseCase refreshUseCase = refreshUseCase();

        var login = loginUseCase.execute(new AuthCommand.Login("buyer@example.com", "secret123"));
        var refreshed = refreshUseCase.execute(login.refreshToken());

        assertEquals("refresh-1", login.refreshToken());
        assertEquals("refresh-2", refreshed.refreshToken());
        assertTrue(jwtTokenProvider.isValid(refreshed.accessToken()));
        // Token cũ bị revoke (soft-delete) — tìm thẳng trong list vì findByToken filter is_deleted=false
        RefreshToken oldToken = refreshTokenRepository.tokens.stream()
                .filter(t -> t.getToken().equals("refresh-1")).findFirst().orElseThrow();
        assertTrue(oldToken.isDeleted());
        assertEquals(2, refreshTokenRepository.tokens.size());
    }

    @Test
    void loginRejectsPasswordUserUntilEmailVerified() {
        userRepository.save(User.createCustomer("buyer@example.com", "encoded:secret123", "Buyer"));
        LoginUseCase loginUseCase = loginUseCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginUseCase.execute(new AuthCommand.Login("buyer@example.com", "secret123")));

        assertEquals("Email is not verified", ex.getMessage());
    }

    @Test
    void registerCreatesUnverifiedCustomerAndQueuesVerificationEmail() {
        RegisterUserUseCase registerUseCase = registerUseCase();

        UUID userId = registerUseCase.execute(new AuthCommand.Register(
                "Buyer@Example.com", "secret123", "Buyer"));

        User user = userRepository.findById(userId).orElseThrow();
        assertEquals("buyer@example.com", user.getEmail());
        assertFalse(user.isEmailVerified());
        EmailVerificationToken token = verificationTokenRepository.tokens.getFirst();
        assertEquals(userId, token.getUserId());
        assertEquals("refresh-1", token.getToken());
        assertEquals("buyer@example.com", notificationFeignClient.lastVerificationRequest.email());
        assertTrue(notificationFeignClient.lastVerificationRequest.verificationLink().contains("token=refresh-1"));
    }

    @Test
    void verifyEmailMarksUserVerifiedAndAllowsLogin() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:secret123", "Buyer"));
        verificationTokenRepository.save(EmailVerificationToken.create(
                user.getId(), "verify-token", Instant.now().plusSeconds(3600)));
        VerifyEmailUseCase verifyEmailUseCase = new VerifyEmailUseCase(
                verificationTokenRepository, userRepository);

        verifyEmailUseCase.execute("verify-token");

        assertTrue(user.isEmailVerified());
        assertTrue(verificationTokenRepository.tokens.getFirst().isUsed());
        assertDoesNotThrow(() -> loginUseCase().execute(new AuthCommand.Login("buyer@example.com", "secret123")));
    }

    // ───────────────────────── Resend Verification ─────────────────────────

    @Test
    void resendVerificationQueuesNewTokenForUnverifiedUser() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:secret123", "Buyer"));
        ResendVerificationEmailUseCase useCase = resendVerificationUseCase();

        useCase.execute("Buyer@Example.com");

        EmailVerificationToken token = verificationTokenRepository.tokens.getLast();
        assertEquals(user.getId(), token.getUserId());
        assertFalse(token.isUsed());
        assertEquals("buyer@example.com", notificationFeignClient.lastVerificationRequest.email());
        assertTrue(notificationFeignClient.lastVerificationRequest.verificationLink().contains("token="));
    }

    @Test
    void resendVerificationIgnoresAlreadyVerifiedUser() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:secret123", "Buyer"));
        user.verifyEmail();
        ResendVerificationEmailUseCase useCase = resendVerificationUseCase();

        useCase.execute("buyer@example.com");

        assertTrue(verificationTokenRepository.tokens.isEmpty());
        assertNull(notificationFeignClient.lastVerificationRequest);
    }

    @Test
    void resendVerificationIgnoresUnknownEmail() {
        ResendVerificationEmailUseCase useCase = resendVerificationUseCase();

        assertDoesNotThrow(() -> useCase.execute("unknown@example.com"));
        assertTrue(verificationTokenRepository.tokens.isEmpty());
        assertNull(notificationFeignClient.lastVerificationRequest);
    }

    @Test
    void resendVerificationIgnoresInactiveUser() {
        User user = userRepository.save(User.createCustomer("inactive@example.com", "encoded:secret123", "Inactive"));
        user.deactivate();
        ResendVerificationEmailUseCase useCase = resendVerificationUseCase();

        useCase.execute("inactive@example.com");

        assertTrue(verificationTokenRepository.tokens.isEmpty());
        assertNull(notificationFeignClient.lastVerificationRequest);
    }

    @Test
    void loginRejectsOAuthOnlyUserWithoutPassword() {
        userRepository.save(User.createOAuthUser("oauth@example.com", "OAuth User", Role.CUSTOMER));
        LoginUseCase loginUseCase = loginUseCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginUseCase.execute(new AuthCommand.Login("oauth@example.com", "secret123")));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void loginRejectsInactiveUser() {
        User user = userRepository.save(User.createCustomer("inactive@example.com", "encoded:pass", "Inactive"));
        user.verifyEmail();
        user.deactivate();
        LoginUseCase loginUseCase = loginUseCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginUseCase.execute(new AuthCommand.Login("inactive@example.com", "pass")));

        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:correctpass", "Buyer"));
        user.verifyEmail();
        LoginUseCase loginUseCase = loginUseCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginUseCase.execute(new AuthCommand.Login("buyer@example.com", "wrongpass")));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void loginRejectsUnknownEmail() {
        LoginUseCase loginUseCase = loginUseCase();

        // Trả cùng message — không lộ email có tồn tại hay không
        BusinessException ex = assertThrows(BusinessException.class,
                () -> loginUseCase.execute(new AuthCommand.Login("nobody@example.com", "pass")));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    // ───────────────────────── Refresh Token ─────────────────────────

    @Test
    void refreshRejectsExpiredToken() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        user.verifyEmail();
        // Lưu trực tiếp token đã expired vào in-memory repo (bypass factory validation)
        RefreshToken expiredToken = createExpiredToken(user.getId(), "expired-token");
        refreshTokenRepository.tokens.add(expiredToken);
        RefreshTokenUseCase refreshUseCase = refreshUseCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshUseCase.execute("expired-token"));

        assertEquals("Refresh token expired", ex.getMessage());
        assertTrue(expiredToken.isDeleted());
    }

    @Test
    void refreshRejectsRevokedToken() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        user.verifyEmail();
        RefreshToken activeToken = refreshTokenRepository.save(
                RefreshToken.create(user.getId(), "active-token", Instant.now().plusSeconds(3600)));
        activeToken.revoke();
        refreshTokenRepository.save(activeToken);
        RefreshTokenUseCase refreshUseCase = refreshUseCase();

        // Token bị revoke → is_deleted=true → @SQLRestriction lọc ra → findByToken trả empty
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshUseCase.execute("active-token"));

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refreshRejectsTokenOfInactiveUser() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        user.verifyEmail();
        refreshTokenRepository.save(
                RefreshToken.create(user.getId(), "valid-token", Instant.now().plusSeconds(3600)));
        user.deactivate();
        RefreshTokenUseCase refreshUseCase = refreshUseCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshUseCase.execute("valid-token"));

        assertTrue(ex.getMessage().contains("not active"));
    }

    // ───────────────────────── Logout ─────────────────────────

    @Test
    void logoutRevokesRefreshToken() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        user.verifyEmail();
        RefreshToken token = refreshTokenRepository.save(
                RefreshToken.create(user.getId(), "logout-token", Instant.now().plusSeconds(3600)));
        LogoutUseCase logoutUseCase = new LogoutUseCase(refreshTokenRepository);

        logoutUseCase.execute("logout-token");

        assertTrue(token.isDeleted());
    }

    @Test
    void logoutRejectsInvalidToken() {
        LogoutUseCase logoutUseCase = new LogoutUseCase(refreshTokenRepository);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> logoutUseCase.execute("non-existent-token"));

        assertEquals("Invalid refresh token", ex.getMessage());
    }

    // ───────────────────────── Forgot Password ─────────────────────────

    @Test
    void forgotPasswordCreatesResetTokenAndQueuesNotification() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:oldpass", "Buyer"));
        ForgotPasswordUseCase useCase = forgotPasswordUseCase();

        useCase.execute("buyer@example.com");

        PasswordResetToken token = resetTokenRepository.tokens.getFirst();
        assertEquals(user.getId(), token.getUserId());
        assertEquals("refresh-1", token.getToken());
        assertEquals("buyer@example.com", notificationFeignClient.lastRequest.email());
        assertTrue(notificationFeignClient.lastRequest.resetLink().contains("token=refresh-1"));
    }

    @Test
    void forgotPasswordSilentlyIgnoresUnknownEmail() {
        // SRS: không tiết lộ email có tồn tại hay không
        ForgotPasswordUseCase useCase = forgotPasswordUseCase();

        assertDoesNotThrow(() -> useCase.execute("unknown@example.com"));
        assertNull(notificationFeignClient.lastRequest);
        assertTrue(resetTokenRepository.tokens.isEmpty());
    }

    @Test
    void forgotPasswordSilentlyIgnoresInactiveUser() {
        User user = userRepository.save(User.createCustomer("inactive@example.com", "encoded:pass", "Inactive"));
        user.deactivate();
        ForgotPasswordUseCase useCase = forgotPasswordUseCase();

        assertDoesNotThrow(() -> useCase.execute("inactive@example.com"));
        assertNull(notificationFeignClient.lastRequest);
        assertTrue(resetTokenRepository.tokens.isEmpty());
    }

    @Test
    void forgotPasswordRevokesOldTokenBeforeCreatingNew() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        PasswordResetToken oldToken = resetTokenRepository.save(
                PasswordResetToken.create(user.getId(), "old-reset-token", Instant.now().plusSeconds(3600)));
        ForgotPasswordUseCase useCase = forgotPasswordUseCase();

        useCase.execute("buyer@example.com");

        assertTrue(oldToken.isUsed(), "Old active reset token should be revoked");
        assertEquals(2, resetTokenRepository.tokens.size());
        assertFalse(resetTokenRepository.tokens.getLast().isUsed(), "New token should not be used");
    }

    // ───────────────────────── Reset Password ─────────────────────────

    @Test
    void resetPasswordChangesPasswordAndRevokesRefreshTokens() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:oldpass", "Buyer"));
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(), "active-refresh", Instant.now().plusSeconds(3600)));
        PasswordResetToken resetToken = resetTokenRepository.save(PasswordResetToken.create(
                user.getId(), "reset-token", Instant.now().plusSeconds(3600)));
        ResetPasswordUseCase useCase = new ResetPasswordUseCase(
                resetTokenRepository, refreshTokenRepository, userRepository, passwordEncoder);

        useCase.execute("reset-token", "newpass123");

        assertEquals("encoded:newpass123", user.getPasswordHash());
        assertTrue(resetToken.isUsed());
        // Tìm thẳng trong list vì findByToken filter is_deleted=false sau khi revoke
        RefreshToken revokedToken = refreshTokenRepository.tokens.stream()
                .filter(t -> t.getToken().equals("active-refresh")).findFirst().orElseThrow();
        assertTrue(revokedToken.isDeleted());
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        PasswordResetToken token = resetTokenRepository.save(
                PasswordResetToken.create(user.getId(), "used-token", Instant.now().plusSeconds(3600)));
        token.markUsed();
        ResetPasswordUseCase useCase = new ResetPasswordUseCase(
                resetTokenRepository, refreshTokenRepository, userRepository, passwordEncoder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute("used-token", "newpass"));

        assertTrue(ex.getMessage().contains("expired or used"));
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        PasswordResetToken expiredToken = createExpiredResetToken(user.getId(), "expired-reset");
        resetTokenRepository.tokens.add(expiredToken);
        ResetPasswordUseCase useCase = new ResetPasswordUseCase(
                resetTokenRepository, refreshTokenRepository, userRepository, passwordEncoder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute("expired-reset", "newpass"));

        assertTrue(ex.getMessage().contains("expired or used"));
    }

    @Test
    void resetPasswordRejectsInvalidToken() {
        ResetPasswordUseCase useCase = new ResetPasswordUseCase(
                resetTokenRepository, refreshTokenRepository, userRepository, passwordEncoder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute("nonexistent-token", "newpass"));

        assertEquals("Invalid password reset token", ex.getMessage());
    }

    @Test
    void resetPasswordRejectsInactiveUser() {
        // Token hợp lệ nhưng user bị deactivate sau khi yêu cầu reset
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        resetTokenRepository.save(PasswordResetToken.create(
                user.getId(), "valid-token", Instant.now().plusSeconds(3600)));
        user.deactivate();
        ResetPasswordUseCase useCase = new ResetPasswordUseCase(
                resetTokenRepository, refreshTokenRepository, userRepository, passwordEncoder);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute("valid-token", "newpass"));

        assertTrue(ex.getMessage().contains("not active"));
        // Password không thay đổi
        assertEquals("encoded:pass", user.getPasswordHash());
    }

    // ───────────────────────── Factory helpers ─────────────────────────

    private LoginUseCase loginUseCase() {
        LoginUseCase useCase = new LoginUseCase(
                userRepository, refreshTokenRepository, passwordEncoder, jwtTokenProvider, tokenGenerator);
        ReflectionTestUtils.setField(useCase, "refreshTtlMs", 604800000L);
        return useCase;
    }

    private RefreshTokenUseCase refreshUseCase() {
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(
                refreshTokenRepository, userRepository, jwtTokenProvider, tokenGenerator);
        ReflectionTestUtils.setField(useCase, "refreshTtlMs", 604800000L);
        return useCase;
    }

    private ForgotPasswordUseCase forgotPasswordUseCase() {
        ForgotPasswordUseCase useCase = new ForgotPasswordUseCase(
                userRepository, resetTokenRepository, notificationFeignClient, tokenGenerator);
        ReflectionTestUtils.setField(useCase, "resetPasswordBaseUrl", "http://localhost/reset-password");
        return useCase;
    }

    private RegisterUserUseCase registerUseCase() {
        RegisterUserUseCase useCase = new RegisterUserUseCase(
                userRepository, passwordEncoder, verificationTokenRepository,
                notificationFeignClient, tokenGenerator);
        ReflectionTestUtils.setField(useCase, "verificationBaseUrl", "http://localhost/verify-email");
        ReflectionTestUtils.setField(useCase, "verificationTtlSeconds", 86400L);
        return useCase;
    }

    private ResendVerificationEmailUseCase resendVerificationUseCase() {
        ResendVerificationEmailUseCase useCase = new ResendVerificationEmailUseCase(
                userRepository, verificationTokenRepository, notificationFeignClient, tokenGenerator);
        ReflectionTestUtils.setField(useCase, "verificationBaseUrl", "http://localhost/verify-email");
        ReflectionTestUtils.setField(useCase, "verificationTtlSeconds", 86400L);
        return useCase;
    }

    /** Tạo RefreshToken đã expired để test mà không cần manipulate time */
    private static RefreshToken createExpiredToken(UUID userId, String tokenValue) {
        try {
            java.lang.reflect.Constructor<RefreshToken> ctor =
                    RefreshToken.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            RefreshToken rt = ctor.newInstance();
            setField(rt, "id", UUID.randomUUID());
            setField(rt, "userId", userId);
            setField(rt, "token", tokenValue);
            setField(rt, "expiresAt", Instant.now().minusSeconds(3600));
            return rt;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Tạo PasswordResetToken đã expired để test mà không cần manipulate time */
    private static PasswordResetToken createExpiredResetToken(UUID userId, String tokenValue) {
        try {
            java.lang.reflect.Constructor<PasswordResetToken> ctor =
                    PasswordResetToken.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            PasswordResetToken prt = ctor.newInstance();
            setField(prt, "id", UUID.randomUUID());
            setField(prt, "userId", userId);
            setField(prt, "token", tokenValue);
            setField(prt, "expiresAt", Instant.now().minusSeconds(3600));
            return prt;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String name, Object value) {
        try {
            // Tìm field trong class và cả superclass
            Class<?> cls = target.getClass();
            java.lang.reflect.Field f = null;
            while (cls != null) {
                try { f = cls.getDeclaredField(name); break; }
                catch (NoSuchFieldException ignored) { cls = cls.getSuperclass(); }
            }
            if (f == null) throw new NoSuchFieldException(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class FakeNotificationFeignClient implements NotificationFeignClient {
        private ResetPasswordEmailRequest lastRequest;
        private VerificationEmailRequest lastVerificationRequest;

        @Override
        public ApiResponse<Void> sendResetPasswordEmail(ResetPasswordEmailRequest request) {
            this.lastRequest = request;
            return ApiResponse.ok(null);
        }

        @Override
        public ApiResponse<Void> sendVerificationEmail(VerificationEmailRequest request) {
            this.lastVerificationRequest = request;
            return ApiResponse.ok(null);
        }
    }

    // Expose tokens list for assertion in tests
    private static class InMemoryPasswordResetTokenRepositoryWithList extends InMemoryPasswordResetTokenRepository {
        // tokens field is already accessible in outer class
    }

    private static class PrefixPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword != null && encodedPassword.equals(encode(rawPassword));
        }
    }

    private static class StubTokenGenerator extends TokenGenerator {
        private int sequence = 1;

        @Override
        public String generateUrlSafeToken() {
            return "refresh-" + sequence++;
        }
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final List<User> users = new ArrayList<>();

        @Override
        public User save(User user) {
            assignIdIfMissing(user);
            users.removeIf(existing -> existing.getId().equals(user.getId()));
            users.add(user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return users.stream().filter(user -> user.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public Page<User> search(UserSearchCriteria criteria, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countActiveAdminsExcluding(UUID excludedUserId) {
            return users.stream()
                    .filter(user -> !user.getId().equals(excludedUserId))
                    .filter(user -> user.getRole() == Role.ADMIN)
                    .filter(User::isActive)
                    .count();
        }
    }

    static class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
        final List<RefreshToken> tokens = new ArrayList<>();

        @Override
        public RefreshToken save(RefreshToken refreshToken) {
            assignIdIfMissing(refreshToken);
            tokens.removeIf(existing -> existing.getId().equals(refreshToken.getId()));
            tokens.add(refreshToken);
            return refreshToken;
        }

        @Override
        public Optional<RefreshToken> findByToken(String token) {
            // Mô phỏng @SQLRestriction("is_deleted = false")
            return tokens.stream()
                    .filter(rt -> rt.getToken().equals(token))
                    .filter(rt -> !rt.isDeleted())
                    .findFirst();
        }

        @Override
        public List<RefreshToken> findByUserId(UUID userId) {
            return tokens.stream().filter(refreshToken -> refreshToken.getUserId().equals(userId)).toList();
        }
    }

    static class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {
        final List<PasswordResetToken> tokens = new ArrayList<>();

        @Override
        public PasswordResetToken save(PasswordResetToken token) {
            assignIdIfMissing(token);
            tokens.removeIf(existing -> existing.getId().equals(token.getId()));
            tokens.add(token);
            return token;
        }

        @Override
        public Optional<PasswordResetToken> findByToken(String token) {
            return tokens.stream().filter(resetToken -> resetToken.getToken().equals(token)).findFirst();
        }

        @Override
        public void revokeActiveByUserId(UUID userId) {
            tokens.stream()
                    .filter(token -> token.getUserId().equals(userId))
                    .filter(token -> !token.isExpired())
                    .filter(token -> !token.isUsed())
                    .forEach(PasswordResetToken::markUsed);
        }
    }

    static class InMemoryEmailVerificationTokenRepository implements EmailVerificationTokenRepository {
        final List<EmailVerificationToken> tokens = new ArrayList<>();

        @Override
        public EmailVerificationToken save(EmailVerificationToken token) {
            assignIdIfMissing(token);
            tokens.removeIf(existing -> existing.getId().equals(token.getId()));
            tokens.add(token);
            return token;
        }

        @Override
        public Optional<EmailVerificationToken> findByToken(String token) {
            return tokens.stream().filter(verificationToken -> verificationToken.getToken().equals(token)).findFirst();
        }

        @Override
        public void revokeActiveByUserId(UUID userId) {
            tokens.stream()
                    .filter(token -> token.getUserId().equals(userId))
                    .filter(token -> !token.isExpired())
                    .filter(token -> !token.isUsed())
                    .forEach(EmailVerificationToken::markUsed);
        }
    }

    private static void assignIdIfMissing(Object entity) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            if (field.get(entity) == null) {
                field.set(entity, UUID.randomUUID());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
