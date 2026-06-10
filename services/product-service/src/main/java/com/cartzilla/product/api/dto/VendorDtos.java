package com.cartzilla.product.api.dto;

import com.cartzilla.product.application.command.VendorCommand;
import com.cartzilla.product.domain.entity.Vendor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request/Response DTO cho vendor (kèm map sang command — F16). */
public class VendorDtos {
    private VendorDtos() {}

    // ─── Requests ──────────────────────────────────────────────────────────

    public record CreateVendorRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 160) String slug,
            @NotBlank String vendorType,
            @Size(max = 255) String contactEmail,
            @Size(max = 20) String phone,
            String website,
            String logoUrl) {
        public VendorCommand.Create toCommand() {
            return new VendorCommand.Create(name, slug, vendorType, contactEmail,
                    phone, website, logoUrl);
        }
    }

    public record UpdateVendorRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 160) String slug,
            @NotBlank String vendorType,
            @Size(max = 255) String contactEmail,
            @Size(max = 20) String phone,
            String website,
            String logoUrl,
            Boolean active) {
        public VendorCommand.Update toCommand() {
            return new VendorCommand.Update(name, slug, vendorType, contactEmail,
                    phone, website, logoUrl, active);
        }
    }

    // ─── Responses ─────────────────────────────────────────────────────────

    public record VendorResponse(
            UUID id, String name, String slug, String vendorType, String contactEmail,
            String phone, String website, String logoUrl, boolean active) {
        public static VendorResponse from(Vendor v) {
            return new VendorResponse(v.getId(), v.getName(), v.getSlug(),
                    v.getVendorType().name(), v.getContactEmail(), v.getPhone(),
                    v.getWebsite(), v.getLogoUrl(), v.isActive());
        }
    }
}
