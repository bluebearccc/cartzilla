package com.cartzilla.user.application.usecase;

import com.cartzilla.security.JwtTokenProvider;
import com.cartzilla.user.domain.entity.OAuthAccount;
import com.cartzilla.user.domain.entity.RefreshToken;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.OAuthAccountRepository;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.UserSearchCriteria;
import com.cartzilla.user.domain.vo.OAuthProvider;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OAuthUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryOAuthAccountRepository oauthAccountRepository = new InMemoryOAuthAccountRepository();
    private final InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
    private final FakeOAuthProviderGateway oauthProviderGateway = new FakeOAuthProviderGateway();
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            "dev-secret-key-change-me-please-32bytes!!", 900000);
    private final StubTokenGenerator tokenGenerator = new StubTokenGenerator();
    private final OAuthStateStore oauthStateStore = new OAuthStateStore(new StubTokenGenerator());

    @Test
    void completeOAuthLogin_createsCustomerAndLinksProviderAccount() {
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "buyer@example.com", "Buyer", "https://cdn/avatar.png", true);
        CompleteOAuthLoginUseCase useCase = useCase();

        var result = useCase.execute(OAuthProvider.GOOGLE, "auth-code", stateFor(OAuthProvider.GOOGLE));

        assertEquals("buyer@example.com", result.email());
        assertEquals(Role.CUSTOMER.name(), result.role());
        assertEquals("refresh-1", result.refreshToken());
        User user = userRepository.findByEmail("buyer@example.com").orElseThrow();
        assertTrue(user.isEmailVerified());
        OAuthAccount account = oauthAccountRepository
                .findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-123").orElseThrow();
        assertEquals(user.getId(), account.getUserId());
        assertNotNull(account.getLastLoginAt());
        assertEquals(1, refreshTokenRepository.findByUserId(user.getId()).size());
    }

    @Test
    void completeOAuthLogin_usesExistingLinkedAccount() {
        User user = userRepository.save(User.createOAuthUser("buyer@example.com", "Buyer", Role.CUSTOMER));
        oauthAccountRepository.save(OAuthAccount.link(
                user.getId(), OAuthProvider.GOOGLE, "google-123", "buyer@example.com", "Buyer", null));
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "buyer@example.com", "Buyer Changed", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        var result = useCase.execute(OAuthProvider.GOOGLE, "auth-code", stateFor(OAuthProvider.GOOGLE));

        assertEquals(user.getId().toString(), jwtTokenProvider.parse(result.accessToken()).getSubject());
        assertEquals(1, userRepository.users.size());
        assertNotNull(oauthAccountRepository.accounts.getFirst().getLastLoginAt());
    }

    @Test
    void completeOAuthLogin_rejectsInactiveExistingEmail() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "hash", "Buyer"));
        user.deactivate();
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "buyer@example.com", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "auth-code", stateFor(OAuthProvider.GOOGLE)));

        assertTrue(ex.getMessage().contains("not active"));
        assertTrue(oauthAccountRepository.accounts.isEmpty());
    }

    // ───────────────────────── validateProfile ─────────────────────────

    @Test
    void completeOAuthLogin_rejectsNullProfile() {
        oauthProviderGateway.profile = null; // gateway trả null (lỗi từ provider)
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "bad-code", stateFor(OAuthProvider.GOOGLE)));

        assertEquals("OAuth profile is missing", ex.getMessage());
    }

    @Test
    void completeOAuthLogin_rejectsMissingProviderUserId() {
        oauthProviderGateway.profile = new OAuthProfile(
                "", "buyer@example.com", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "code", stateFor(OAuthProvider.GOOGLE)));

        assertEquals("OAuth provider user id is missing", ex.getMessage());
    }

    @Test
    void completeOAuthLogin_rejectsMissingEmail() {
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "code", stateFor(OAuthProvider.GOOGLE)));

        assertEquals("OAuth email is missing", ex.getMessage());
    }

    @Test
    void completeOAuthLogin_rejectsUnverifiedEmail() {
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "buyer@example.com", "Buyer", null, false); // emailVerified=false
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "code", stateFor(OAuthProvider.GOOGLE)));

        assertEquals("OAuth email is not verified", ex.getMessage());
    }

    // ───────────────────────── findOrCreateUser ─────────────────────────

    @Test
    void completeOAuthLogin_linksNewProviderToExistingEmailUser() {
        // User đã tồn tại qua email (e.g. đăng ký thường) — chưa link Google bao giờ
        userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        oauthProviderGateway.profile = new OAuthProfile(
                "google-xyz", "buyer@example.com", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        var result = useCase.execute(OAuthProvider.GOOGLE, "auth-code", stateFor(OAuthProvider.GOOGLE));

        assertEquals("buyer@example.com", result.email());
        assertEquals(1, userRepository.users.size(), "Không tạo user mới");
        OAuthAccount linked = oauthAccountRepository
                .findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-xyz").orElseThrow();
        assertNotNull(linked);
    }

    @Test
    void completeOAuthLogin_rejectsAlreadyLinkedProviderForSameEmail() {
        User user = userRepository.save(User.createCustomer("buyer@example.com", "encoded:pass", "Buyer"));
        // User đã link Google với provider ID khác trước đó
        oauthAccountRepository.save(OAuthAccount.link(
                user.getId(), OAuthProvider.GOOGLE, "old-google-id",
                "buyer@example.com", "Buyer", null));
        // Bây giờ callback với  providerUserId mới (khác old-google-id)
        oauthProviderGateway.profile = new OAuthProfile(
                "new-google-id", "buyer@example.com", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "code", stateFor(OAuthProvider.GOOGLE)));

        assertEquals("OAuth provider already linked to this user", ex.getMessage());
    }

    @Test
    void completeOAuthLogin_rejectsLinkedUserWhoIsNowInactive() {
        // Lần 1: user link thành công, sau đó admin deactivate user
        User user = userRepository.save(User.createOAuthUser("buyer@example.com", "Buyer", Role.CUSTOMER));
        oauthAccountRepository.save(OAuthAccount.link(
                user.getId(), OAuthProvider.GOOGLE, "google-123",
                "buyer@example.com", "Buyer", null));
        user.deactivate();
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "buyer@example.com", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "auth-code", stateFor(OAuthProvider.GOOGLE)));

        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    void completeOAuthLogin_rejectsInvalidState() {
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "buyer@example.com", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "auth-code", "unknown-state"));

        assertEquals("Invalid OAuth state", ex.getMessage());
        assertTrue(refreshTokenRepository.findByUserId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void completeOAuthLogin_rejectsReusedState() {
        oauthProviderGateway.profile = new OAuthProfile(
                "google-123", "buyer@example.com", "Buyer", null, true);
        CompleteOAuthLoginUseCase useCase = useCase();
        String state = stateFor(OAuthProvider.GOOGLE);

        useCase.execute(OAuthProvider.GOOGLE, "auth-code", state);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(OAuthProvider.GOOGLE, "auth-code", state));

        assertEquals("Invalid OAuth state", ex.getMessage());
    }

    private CompleteOAuthLoginUseCase useCase() {
        CompleteOAuthLoginUseCase useCase = new CompleteOAuthLoginUseCase(
                userRepository, oauthAccountRepository, refreshTokenRepository,
                oauthProviderGateway, oauthStateStore, jwtTokenProvider, tokenGenerator);
        ReflectionTestUtils.setField(useCase, "refreshTtlMs", 604800000L);
        return useCase;
    }

    private String stateFor(OAuthProvider provider) {
        return oauthStateStore.issue(provider);
    }

    private static class FakeOAuthProviderGateway implements OAuthProviderGateway {
        private OAuthProfile profile;

        @Override
        public String buildAuthorizationUrl(OAuthProvider provider, String state) {
            return "https://auth.example.com?state=" + state;
        }

        @Override
        public OAuthProfile fetchProfile(OAuthProvider provider, String code) {
            return profile;
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
        public Optional<User> findByPhone(String phone) {
            return users.stream().filter(user -> phone.equals(user.getPhone())).findFirst();
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

    private static class InMemoryOAuthAccountRepository implements OAuthAccountRepository {
        private final List<OAuthAccount> accounts = new ArrayList<>();

        @Override
        public OAuthAccount save(OAuthAccount account) {
            assignIdIfMissing(account);
            accounts.removeIf(existing -> existing.getId().equals(account.getId()));
            accounts.add(account);
            return account;
        }

        @Override
        public Optional<OAuthAccount> findByProviderAndProviderUserId(
                OAuthProvider provider, String providerUserId) {
            return accounts.stream()
                    .filter(account -> account.getProvider() == provider)
                    .filter(account -> account.getProviderUserId().equals(providerUserId))
                    .findFirst();
        }

        @Override
        public Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider) {
            return accounts.stream()
                    .filter(account -> account.getUserId().equals(userId))
                    .filter(account -> account.getProvider() == provider)
                    .findFirst();
        }
    }

    private static class InMemoryRefreshTokenRepository implements RefreshTokenRepository {
        private final List<RefreshToken> tokens = new ArrayList<>();

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
            return tokens.stream()
                    .filter(refreshToken -> refreshToken.getUserId().equals(userId))
                    .sorted(Comparator.comparing(RefreshToken::getExpiresAt))
                    .toList();
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
