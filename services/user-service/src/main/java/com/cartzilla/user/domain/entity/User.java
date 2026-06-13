package com.cartzilla.user.domain.entity;

import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Aggregate root — UserAggregate.
 * Rules: UA-01..UA-08, U-01..U-04.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Nullable nếu user chỉ đăng nhập OAuth — UA-01 */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** U-02: chỉ set true sau xác thực email hoặc OAuth verified */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /** UA-04: chỉ isActive=true mới được checkout/voucher/refresh token */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Factory: tạo Customer mới — UA-03, U-01 */
    public static User createCustomer(String email, String passwordHash, String fullName) {
        if (email == null || email.isBlank())
            throw new BusinessException("Email must not be blank");
        if (passwordHash == null || passwordHash.isBlank())
            throw new BusinessException("Password hash must not be blank");
        User u = new User();
        u.email = email.trim().toLowerCase();
        u.passwordHash = passwordHash;
        u.fullName = fullName;
        u.role = Role.CUSTOMER;
        u.active = true;
        u.emailVerified = false;
        return u;
    }

    /** Tạo user OAuth-only (không có password) — UA-01 */
    public static User createOAuthUser(String email, String fullName, Role role) {
        if (email == null || email.isBlank())
            throw new BusinessException("Email must not be blank");
        User u = new User();
        u.email = email.trim().toLowerCase();
        u.fullName = fullName;
        u.role = role != null ? role : Role.CUSTOMER;
        u.active = true;
        // OAuth provider đã xác minh email
        u.emailVerified = true;
        return u;
    }

    /** U-03: Đổi email → reset emailVerified */
    public void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.isBlank())
            throw new BusinessException("Email must not be blank");
        this.email = newEmail.trim().toLowerCase();
        this.emailVerified = false;
    }

    public void changePassword(String newHash) {
        if (newHash == null || newHash.isBlank())
            throw new BusinessException("Password hash must not be blank");
        this.passwordHash = newHash;
    }

    public void updateProfile(String fullName, String phone) {
        if (fullName == null || fullName.isBlank())
            throw new BusinessException("fullName must not be blank");
        this.fullName = fullName.trim();
        this.phone = phone == null || phone.isBlank() ? null : phone.trim();
    }

    public void changeRole(Role role) {
        if (role == null)
            throw new BusinessException("role must not be null");
        this.role = role;
    }

    public void verifyEmail() { this.emailVerified = true; }

    public void deactivate() { this.active = false; }

    public void activate() { this.active = true; }

    /** UA-04: guard — ném exception nếu user không active */
    public void requireActive() {
        if (!active)
            throw new BusinessException("User account is not active");
    }
}
