package com.cartzilla.user.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Child entity của UserAggregate.
 * Rules: A-01..A-05, UA-05, UA-06.
 */
@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** A-02 — FK → users.id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** A-01 */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** A-01 */
    @Column(nullable = false, length = 20)
    private String phone;

    /** A-01 */
    @Column(nullable = false)
    private String street;

    /** A-01 */
    @Column(nullable = false, length = 100)
    private String district;

    /** A-01 */
    @Column(nullable = false, length = 100)
    private String city;

    /** UA-05: chỉ một địa chỉ default per user */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /** Factory — A-01: tất cả required fields NOT NULL */
    public static Address create(UUID userId, String fullName, String phone,
                                  String street, String district, String city,
                                  boolean isDefault) {
        validateRequired(fullName, "fullName");
        validateRequired(phone, "phone");
        validateRequired(street, "street");
        validateRequired(district, "district");
        validateRequired(city, "city");
        if (userId == null)
            throw new BusinessException("userId must not be null");
        Address a = new Address();
        a.userId = userId;
        a.fullName = fullName.trim();
        a.phone = phone.trim();
        a.street = street.trim();
        a.district = district.trim();
        a.city = city.trim();
        a.isDefault = isDefault;
        return a;
    }

    /** UA-05: unset default — gọi trước khi set address khác làm default */
    public void unsetDefault() { this.isDefault = false; }

    /** UA-05 */
    public void setAsDefault() { this.isDefault = true; }

    private static void validateRequired(String val, String field) {
        if (val == null || val.isBlank())
            throw new BusinessException(field + " must not be blank");
    }
}
