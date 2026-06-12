package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.entity.VoucherAllowedUser;
import com.cartzilla.user.domain.entity.VoucherUsage;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.user.domain.repository.VoucherUsageRepository;
import com.cartzilla.user.domain.vo.DiscountType;
import com.cartzilla.user.domain.vo.VoucherAudienceType;
import com.cartzilla.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VoucherUseCaseTest {

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryVoucherRepository voucherRepository = new InMemoryVoucherRepository();
    private final InMemoryVoucherUsageRepository usageRepository = new InMemoryVoucherUsageRepository();
    private final InMemoryVoucherAllowedUserRepository allowedUserRepository = new InMemoryVoucherAllowedUserRepository();

    @Test
    void validateVoucher_capsPercentageDiscountAtMaxDiscountAmount() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        setCreatedAt(user, Instant.now().minus(40, ChronoUnit.DAYS));
        voucherRepository.save(voucher("SUMMER20", DiscountType.PERCENTAGE, "20", "150000",
                VoucherAudienceType.ALL_USERS, 0, 10, 1));
        ValidateVoucherUseCase useCase = new ValidateVoucherUseCase(
                voucherRepository, userRepository, usageRepository, allowedUserRepository);

        var result = useCase.execute(new VoucherCommand.Validate(
                "summer20", user.getId(), new BigDecimal("1000000")));

        assertEquals("SUMMER20", result.code());
        assertEquals(0, new BigDecimal("150000").compareTo(result.discountAmount()));
        assertTrue(result.valid());
        assertEquals(0, voucherRepository.findByCode("SUMMER20").orElseThrow().getUsedCount());
    }

    @Test
    void validateVoucher_rejectsWhenAccountTooYoung() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        setCreatedAt(user, Instant.now().minus(5, ChronoUnit.DAYS));
        voucherRepository.save(voucher("LOYAL10", DiscountType.PERCENTAGE, "10", "100000",
                VoucherAudienceType.LOYAL_CUSTOMER, 30, 10, 1));
        ValidateVoucherUseCase useCase = new ValidateVoucherUseCase(
                voucherRepository, userRepository, usageRepository, allowedUserRepository);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(
                new VoucherCommand.Validate("LOYAL10", user.getId(), new BigDecimal("500000"))));

        assertTrue(ex.getMessage().contains("account age"));
    }

    @Test
    void validateVoucher_rejectsSpecificUsersAudienceMismatch() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        setCreatedAt(user, Instant.now().minus(40, ChronoUnit.DAYS));
        voucherRepository.save(voucher("PRIVATE10", DiscountType.PERCENTAGE, "10", "100000",
                VoucherAudienceType.SPECIFIC_USERS, 0, 10, 1));
        ValidateVoucherUseCase useCase = new ValidateVoucherUseCase(
                voucherRepository, userRepository, usageRepository, allowedUserRepository);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(
                new VoucherCommand.Validate("PRIVATE10", user.getId(), new BigDecimal("500000"))));

        assertTrue(ex.getMessage().contains("audience"));
    }

    @Test
    void redeemVoucher_isIdempotentByVoucherAndOrder() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        setCreatedAt(user, Instant.now().minus(40, ChronoUnit.DAYS));
        Voucher voucher = voucherRepository.save(voucher("SAVE50K", DiscountType.FIXED_AMOUNT, "50000", null,
                VoucherAudienceType.ALL_USERS, 0, 10, 1));
        RedeemVoucherUseCase useCase = new RedeemVoucherUseCase(
                voucherRepository, userRepository, usageRepository, allowedUserRepository);
        UUID orderId = UUID.randomUUID();

        var first = useCase.execute(new VoucherCommand.Redeem(
                "SAVE50K", user.getId(), orderId, new BigDecimal("300000")));
        var second = useCase.execute(new VoucherCommand.Redeem(
                "SAVE50K", user.getId(), orderId, new BigDecimal("300000")));

        assertEquals(first.usageId(), second.usageId());
        assertEquals(1, usageRepository.countByVoucherIdAndUserId(voucher.getId(), user.getId()));
        assertEquals(1, voucherRepository.findById(voucher.getId()).orElseThrow().getUsedCount());
        assertTrue(second.idempotent());
    }

    @Test
    void createVoucher_rejectsDuplicateCodeIgnoringCase() {
        voucherRepository.save(voucher("SUMMER10", DiscountType.PERCENTAGE, "10", "100000",
                VoucherAudienceType.ALL_USERS, 0, 10, 1));
        CreateVoucherUseCase useCase = new CreateVoucherUseCase(voucherRepository);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(
                new VoucherCommand.Create(
                        "summer10",
                        DiscountType.PERCENTAGE,
                        new BigDecimal("10"),
                        new BigDecimal("100000"),
                        BigDecimal.ZERO,
                        10,
                        Instant.now().minus(1, ChronoUnit.DAYS),
                        Instant.now().plus(30, ChronoUnit.DAYS),
                        0,
                        1,
                        VoucherAudienceType.ALL_USERS,
                        false,
                        0,
                        BigDecimal.ZERO)));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void addAllowedUser_rejectsDuplicateUserForVoucher() {
        User user = userRepository.save(User.createCustomer("customer@example.com", "hash", "Customer"));
        Voucher voucher = voucherRepository.save(voucher("PRIVATE10", DiscountType.PERCENTAGE, "10", "100000",
                VoucherAudienceType.SPECIFIC_USERS, 0, 10, 1));
        AddVoucherAllowedUserUseCase useCase = new AddVoucherAllowedUserUseCase(
                voucherRepository, userRepository, allowedUserRepository);

        useCase.execute(voucher.getId(), user.getId());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(voucher.getId(), user.getId()));

        assertTrue(ex.getMessage().contains("already allowed"));
    }

    private static Voucher voucher(String code, DiscountType type, String value, String maxDiscount,
                                   VoucherAudienceType audienceType, int minAgeDays,
                                   int maxUses, int perUserLimit) {
        return Voucher.create(
                code,
                type,
                new BigDecimal(value),
                maxDiscount == null ? null : new BigDecimal(maxDiscount),
                BigDecimal.ZERO,
                maxUses,
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(30, ChronoUnit.DAYS),
                minAgeDays,
                perUserLimit,
                audienceType,
                false,
                0,
                BigDecimal.ZERO);
    }

    private static void setCreatedAt(Object target, Instant createdAt) {
        setFieldInHierarchy(target, "createdAt", createdAt);
    }

    private static void assignIdIfMissing(Object target) {
        if (getFieldInHierarchy(target, "id") == null) {
            setFieldInHierarchy(target, "id", UUID.randomUUID());
        }
    }

    private static Object getFieldInHierarchy(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setFieldInHierarchy(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
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
    }

    private static class InMemoryVoucherRepository implements VoucherRepository {
        private final List<Voucher> vouchers = new ArrayList<>();

        @Override
        public Voucher save(Voucher voucher) {
            assignIdIfMissing(voucher);
            vouchers.removeIf(existing -> existing.getId().equals(voucher.getId()));
            vouchers.add(voucher);
            return voucher;
        }

        @Override
        public Optional<Voucher> findById(UUID id) {
            return vouchers.stream().filter(voucher -> voucher.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Voucher> findByCode(String code) {
            return vouchers.stream().filter(voucher -> voucher.getCode().equalsIgnoreCase(code)).findFirst();
        }

        @Override
        public List<Voucher> findAll() {
            return List.copyOf(vouchers);
        }

        @Override
        public boolean existsByCode(String code) {
            return findByCode(code).isPresent();
        }

        @Override
        public int incrementUsedCountIfAvailable(UUID id) {
            Voucher voucher = findById(id).orElseThrow();
            if (voucher.getUsedCount() >= voucher.getMaxUses()) {
                return 0;
            }
            voucher.incrementUsedCount();
            return 1;
        }
    }

    private static class InMemoryVoucherUsageRepository implements VoucherUsageRepository {
        private final List<VoucherUsage> usages = new ArrayList<>();

        @Override
        public VoucherUsage save(VoucherUsage usage) {
            assignIdIfMissing(usage);
            usages.removeIf(existing -> existing.getId().equals(usage.getId()));
            usages.add(usage);
            return usage;
        }

        @Override
        public Optional<VoucherUsage> findByVoucherIdAndOrderId(UUID voucherId, UUID orderId) {
            return usages.stream()
                    .filter(usage -> usage.getVoucherId().equals(voucherId) && usage.getOrderId().equals(orderId))
                    .findFirst();
        }

        @Override
        public long countByVoucherIdAndUserId(UUID voucherId, UUID userId) {
            return usages.stream()
                    .filter(usage -> usage.getVoucherId().equals(voucherId) && usage.getUserId().equals(userId))
                    .count();
        }
    }

    private static class InMemoryVoucherAllowedUserRepository implements VoucherAllowedUserRepository {
        private final List<VoucherAllowedUser> allowedUsers = new ArrayList<>();

        @Override
        public VoucherAllowedUser save(VoucherAllowedUser allowedUser) {
            assignIdIfMissing(allowedUser);
            allowedUsers.removeIf(existing -> existing.getId().equals(allowedUser.getId()));
            allowedUsers.add(allowedUser);
            return allowedUser;
        }

        @Override
        public boolean existsByVoucherIdAndUserId(UUID voucherId, UUID userId) {
            return allowedUsers.stream()
                    .anyMatch(allowed -> allowed.getVoucherId().equals(voucherId)
                            && allowed.getUserId().equals(userId));
        }

        @Override
        public List<VoucherAllowedUser> findByVoucherId(UUID voucherId) {
            return allowedUsers.stream()
                    .filter(allowed -> allowed.getVoucherId().equals(voucherId))
                    .toList();
        }

        @Override
        public void deleteByVoucherIdAndUserId(UUID voucherId, UUID userId) {
            allowedUsers.removeIf(allowed -> allowed.getVoucherId().equals(voucherId)
                    && allowed.getUserId().equals(userId));
        }
    }
}
