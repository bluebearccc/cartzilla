package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AdminUserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.UserSearchCriteria;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdminUserUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();

    @Test
    void listUsers_returnsPagedUsersOrderedByEmail() {
        userRepository.save(User.createCustomer("z@example.com", "hash", "Zed"));
        userRepository.save(User.createCustomer("a@example.com", "hash", "Ann"));
        ListUsersUseCase useCase = new ListUsersUseCase(userRepository);

        Page<User> users = useCase.execute(new AdminUserCommand.Search(null, null, null), 0, 1, "email,asc");

        assertEquals(2, users.getTotalElements());
        assertEquals(2, users.getTotalPages());
        assertEquals("a@example.com", users.getContent().getFirst().getEmail());
    }

    @Test
    void listUsers_filtersByKeywordRoleAndStatus() {
        User customer = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        customer.deactivate();
        userRepository.save(User.createOAuthUser("staff@example.com", "Staff Member", Role.STAFF));
        userRepository.save(User.createOAuthUser("admin@example.com", "Admin", Role.ADMIN));
        ListUsersUseCase useCase = new ListUsersUseCase(userRepository);

        Page<User> users = useCase.execute(new AdminUserCommand.Search("staff", Role.STAFF, true),
                0, 20, "email,asc");

        assertEquals(1, users.getTotalElements());
        assertEquals("staff@example.com", users.getContent().getFirst().getEmail());
    }

    @Test
    void getUser_returnsExistingUser() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        GetUserUseCase useCase = new GetUserUseCase(userRepository);

        User result = useCase.execute(user.getId());

        assertEquals(user.getId(), result.getId());
        assertEquals("customer@example.com", result.getEmail());
    }

    @Test
    void updateUserRole_promotesCustomerToStaff() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        UpdateUserRoleUseCase useCase = new UpdateUserRoleUseCase(userRepository);

        User updated = useCase.execute(user.getId(), new AdminUserCommand.UpdateRole(Role.STAFF));

        assertEquals(Role.STAFF, updated.getRole());
    }

    @Test
    void updateUserRole_rejectsDemotingLastAdmin() {
        User admin = userRepository.save(User.createOAuthUser("admin@example.com", "Admin", Role.ADMIN));
        UpdateUserRoleUseCase useCase = new UpdateUserRoleUseCase(userRepository);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(
                admin.getId(), new AdminUserCommand.UpdateRole(Role.STAFF)));

        assertTrue(ex.getMessage().contains("last admin"));
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    void updateUserStatus_deactivatesAndActivatesUser() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        UpdateUserStatusUseCase useCase = new UpdateUserStatusUseCase(userRepository);

        User inactive = useCase.execute(user.getId(), new AdminUserCommand.UpdateStatus(false));
        assertFalse(inactive.isActive());

        User active = useCase.execute(user.getId(), new AdminUserCommand.UpdateStatus(true));
        assertTrue(active.isActive());
    }

    @Test
    void updateUserStatus_rejectsDeactivatingLastAdmin() {
        User admin = userRepository.save(User.createOAuthUser("admin@example.com", "Admin", Role.ADMIN));
        UpdateUserStatusUseCase useCase = new UpdateUserStatusUseCase(userRepository);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(
                admin.getId(), new AdminUserCommand.UpdateStatus(false)));

        assertTrue(ex.getMessage().contains("last admin"));
        assertTrue(admin.isActive());
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
        public Page<User> search(UserSearchCriteria criteria, org.springframework.data.domain.Pageable pageable) {
            List<User> filtered = users.stream()
                    .filter(user -> criteria.keyword() == null
                            || user.getEmail().contains(criteria.keyword().toLowerCase())
                            || user.getFullName().toLowerCase().contains(criteria.keyword().toLowerCase()))
                    .filter(user -> criteria.role() == null || user.getRole() == criteria.role())
                    .filter(user -> criteria.active() == null || user.isActive() == criteria.active())
                    .sorted(Comparator.comparing(User::getEmail))
                    .toList();
            int start = Math.min((int) pageable.getOffset(), filtered.size());
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            return new org.springframework.data.domain.PageImpl<>(
                    filtered.subList(start, end), pageable, filtered.size());
        }

        @Override
        public long countActiveAdminsExcluding(UUID excludedUserId) {
            return users.stream()
                    .filter(user -> !user.getId().equals(excludedUserId))
                    .filter(user -> user.getRole() == Role.ADMIN)
                    .filter(User::isActive)
                    .count();
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
