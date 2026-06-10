package com.cartzilla.order.domain.vo;

import com.cartzilla.web.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * VO: ShippingAddress — snapshot JSONB; OA-04, BR05.
 * Immutable sau khi tạo order.
 */
public final class ShippingAddress {

    private final String fullName;
    private final String phone;
    private final String street;
    private final String district;
    private final String city;

    @JsonCreator
    public ShippingAddress(@JsonProperty("fullName") String fullName,
                            @JsonProperty("phone") String phone,
                            @JsonProperty("street") String street,
                            @JsonProperty("district") String district,
                            @JsonProperty("city") String city) {
        validate(fullName, "fullName");
        validate(phone, "phone");
        validate(street, "street");
        validate(district, "district");
        validate(city, "city");
        this.fullName = fullName.trim();
        this.phone = phone.trim();
        this.street = street.trim();
        this.district = district.trim();
        this.city = city.trim();
    }

    private static void validate(String val, String field) {
        if (val == null || val.isBlank())
            throw new BusinessException("ShippingAddress." + field + " must not be blank");
    }

    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getStreet() { return street; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
}
