package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.UserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProfileUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();

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

        private static void assignIdIfMissing(User user) {
            try {
                Field field = User.class.getDeclaredField("id");
                field.setAccessible(true);
                if (field.get(user) == null) {
                    field.set(user, UUID.randomUUID());
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
