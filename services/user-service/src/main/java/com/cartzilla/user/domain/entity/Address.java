package com.cartzilla.user.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    public static Address create(UUID userId, String fullName, String phone,
                                 String street, String district, String city,
                                 boolean isDefault) {
        validateRequired(fullName, "fullName");
        validateRequired(phone, "phone");
        validateRequired(street, "street");
        validateRequired(district, "district");
        validateRequired(city, "city");
        if (userId == null) {
            throw new BusinessException("userId must not be null");
        }

        Address address = new Address();
        address.userId = userId;
        address.fullName = fullName.trim();
        address.phone = phone.trim();
        address.street = street.trim();
        address.district = district.trim();
        address.city = city.trim();
        address.isDefault = isDefault;
        return address;
    }

    public void updateDetails(String fullName, String phone, String street,
                              String district, String city) {
        validateRequired(fullName, "fullName");
        validateRequired(phone, "phone");
        validateRequired(street, "street");
        validateRequired(district, "district");
        validateRequired(city, "city");
        this.fullName = fullName.trim();
        this.phone = phone.trim();
        this.street = street.trim();
        this.district = district.trim();
        this.city = city.trim();
    }

    public void unsetDefault() {
        this.isDefault = false;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    private static void validateRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " must not be blank");
        }
    }
}
