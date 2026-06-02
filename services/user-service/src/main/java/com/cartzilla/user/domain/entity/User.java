package com.cartzilla.user.domain.entity;

import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

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

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Factory: tạo Customer mới. */
    public static User createCustomer(String email, String passwordHash, String fullName) {
        User u = new User();
        u.email = email;
        u.passwordHash = passwordHash;
        u.fullName = fullName;
        u.role = Role.CUSTOMER;
        u.active = true;
        return u;
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }
}
