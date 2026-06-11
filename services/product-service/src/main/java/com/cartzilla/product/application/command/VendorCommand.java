package com.cartzilla.product.application.command;

/** Input commands cho vendor use cases (F16 — UC-05). */
public class VendorCommand {
    private VendorCommand() {}

    public record Create(
            String name,
            String slug,
            String vendorType,
            String contactEmail,
            String phone,
            String website,
            String logoUrl) {}

    public record Update(
            String name,
            String slug,
            String vendorType,
            String contactEmail,
            String phone,
            String website,
            String logoUrl,
            Boolean active) {}
}
