package com.cartzilla.user.application.command;

public class AddressCommand {
    private AddressCommand() {}

    public record Create(
            String fullName,
            String phone,
            String street,
            String district,
            String city,
            boolean defaultAddress) {}

    public record Update(
            String fullName,
            String phone,
            String street,
            String district,
            String city,
            Boolean defaultAddress) {}
}
