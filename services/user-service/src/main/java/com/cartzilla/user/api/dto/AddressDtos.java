package com.cartzilla.user.api.dto;

import com.cartzilla.user.application.command.AddressCommand;
import com.cartzilla.user.domain.entity.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AddressDtos {
    private AddressDtos() {}

    public record CreateAddressRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 255) String street,
            @NotBlank @Size(max = 100) String district,
            @NotBlank @Size(max = 100) String city,
            boolean defaultAddress) {
        public AddressCommand.Create toCommand() {
            return new AddressCommand.Create(fullName, phone, street, district, city, defaultAddress);
        }
    }

    public record UpdateAddressRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 255) String street,
            @NotBlank @Size(max = 100) String district,
            @NotBlank @Size(max = 100) String city,
            Boolean defaultAddress) {
        public AddressCommand.Update toCommand() {
            return new AddressCommand.Update(fullName, phone, street, district, city, defaultAddress);
        }
    }

    public record AddressResponse(
            UUID id,
            String fullName,
            String phone,
            String street,
            String district,
            String city,
            boolean defaultAddress) {
        public static AddressResponse from(Address address) {
            return new AddressResponse(
                    address.getId(),
                    address.getFullName(),
                    address.getPhone(),
                    address.getStreet(),
                    address.getDistrict(),
                    address.getCity(),
                    address.isDefault());
        }
    }
}
