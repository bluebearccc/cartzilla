package com.cartzilla.product.domain.entity;

import com.cartzilla.product.domain.vo.VendorType;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Aggregate root — VendorAggregate.
 * Rules: VE-01..VE-03, VN-01..VN-02.
 */
@Entity
@Table(name = "vendors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Vendor extends BaseEntity {

    /** VN-02 */
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** VN-01: NOT NULL, max 150 */
    @Column(nullable = false, length = 150)
    private String name;

    /** VE-01: unique */
    @Column(nullable = false, length = 160, unique = true)
    private String slug;

    /** VE-02 */
    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_type", nullable = false, length = 20)
    private VendorType vendorType;

    /** VN-02: validate via Email VO nếu có */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String website;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(nullable = false)
    private boolean active = true;

    public static Vendor create(String name, String slug, VendorType vendorType,
                                 String contactEmail, String phone, String website, String logoUrl) {
        if (name == null || name.isBlank())
            throw new BusinessException("Vendor name must not be blank (VN-01)");
        if (slug == null || slug.isBlank())
            throw new BusinessException("Vendor slug must not be blank (VE-01)");
        if (vendorType == null)
            throw new BusinessException("Vendor type must not be null (VE-02)");
        Vendor v = new Vendor();
        v.name = name.trim();
        v.slug = slug.trim().toLowerCase();
        v.vendorType = vendorType;
        v.contactEmail = validateEmail(contactEmail);
        v.phone = phone;
        v.website = website;
        v.logoUrl = logoUrl;
        v.active = true;
        return v;
    }

    /** VN-01/VN-02/VE-02: admin cập nhật thông tin chung */
    public void update(String name, VendorType vendorType, String contactEmail,
                       String phone, String website, String logoUrl) {
        if (name == null || name.isBlank())
            throw new BusinessException("Vendor name must not be blank (VN-01)");
        if (vendorType == null)
            throw new BusinessException("Vendor type must not be null (VE-02)");
        this.name = name.trim();
        this.vendorType = vendorType;
        this.contactEmail = validateEmail(contactEmail);
        this.phone = phone;
        this.website = website;
        this.logoUrl = logoUrl;
    }

    /** VE-01: slug unique — usecase check trùng trước khi đổi */
    public void changeSlug(String slug) {
        if (slug == null || slug.isBlank())
            throw new BusinessException("Vendor slug must not be blank (VE-01)");
        this.slug = slug.trim().toLowerCase();
    }

    /** VE-03: deactivate không xóa, chỉ ngừng gán product mới */
    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }

    /** VN-02: contactEmail nếu có phải đúng format */
    private static String validateEmail(String email) {
        if (email == null || email.isBlank()) return null;
        if (!EMAIL.matcher(email.trim()).matches())
            throw new BusinessException("Invalid contact email (VN-02): " + email);
        return email.trim();
    }
}
