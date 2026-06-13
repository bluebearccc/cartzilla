package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.UserCommand;
import com.cartzilla.user.domain.entity.RefreshToken;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.UserSearchCriteria;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
    private final PasswordEncoder passwordEncoder = new PrefixPasswordEncoder();

    @Test
    void getProfile_rejectsInactiveUser() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        user.deactivate();
        GetProfileUseCase useCase = new GetProfileUseCase(userRepository);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(user.getId()));

        assertTrue(ex.getMessage().contains("not active"));
    }

    @Test
    void updateProfile_updatesMutableFields() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        UpdateProfileUseCase useCase = new UpdateProfileUseCase(userRepository);

        User updated = useCase.execute(user.getId(), new UserCommand.UpdateProfile(
                "Customer Updated",
                "0900000000"));

        assertEquals("Customer Updated", updated.getFullName());
        assertEquals("0900000000", updated.getPhone());
        assertEquals("customer@example.com", updated.getEmail());
    }

    @Test
    void updateProfile_rejectsBlankFullName() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        UpdateProfileUseCase useCase = new UpdateProfileUseCase(userRepository);

        assertThrows(BusinessException.class, () -> useCase.execute(
                user.getId(),
                new UserCommand.UpdateProfile(" ", "0900000000")));
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndRevokesRefreshTokens() {
        User user = userRepository.save(User.createCustomer(
                "customer@example.com", "encoded:oldpass", "Customer"));
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(), "active-refresh", Instant.now().plusSeconds(3600)));
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(
                userRepository, refreshTokenRepository, passwordEncoder);

        useCase.execute(user.getId(), new UserCommand.ChangePassword("oldpass", "newpass123"));

        assertEquals("encoded:newpass123", user.getPasswordHash());
        assertTrue(refreshTokenRepository.findByToken("active-refresh").orElseThrow().isDeleted());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = userRepository.save(User.createCustomer(
                "customer@example.com", "encoded:oldpass", "Customer"));
        ChangePasswordUseCase useCase = new ChangePasswordUseCase(
                userRepository, refreshTokenRepository, passwordEncoder);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(
                user.getId(), new UserCommand.ChangePassword("wrong", "newpass123")));

        assertTrue(ex.getMessage().contains("Current password"));
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
            List<User> filtered = users.stream()
                    .filter(user -> criteria.normalizedKeyword() == null
                            || user.getEmail().contains(criteria.normalizedKeyword())
                            || user.getFullName().toLowerCase().contains(criteria.normalizedKeyword()))
                    .filter(user -> criteria.role() == null || user.getRole() == criteria.role())
                    .filter(user -> criteria.active() == null || user.isActive() == criteria.active())
                    .sorted(Comparator.comparing(User::getEmail))
                    .toList();
            return new PageImpl<>(filtered, pageable, filtered.size());
        }

        @Override
        public long countActiveAdminsExcluding(UUID excludedUserId) {
            return users.stream()
                    .filter(user -> !user.getId().equals(excludedUserId))
                    .filter(user -> user.getRole() == Role.ADMIN)
                    .filter(User::isActive)
                    .count();
        }

        private static void assignIdIfMissing(Object target) {
            ProfileUseCaseTest.assignIdIfMissing(target);
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
            return tokens.stream().filter(refreshToken -> refreshToken.getToken().equals(token)).findFirst();
        }

        @Override
        public List<RefreshToken> findByUserId(UUID userId) {
            return tokens.stream().filter(refreshToken -> refreshToken.getUserId().equals(userId)).toList();
        }
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

    private static void assignIdIfMissing(Object target) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            if (field.get(target) == null) {
                field.set(target, UUID.randomUUID());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
