package com.cartzilla.user.api.controller;

import com.cartzilla.user.api.ApiPaths;
import com.cartzilla.user.domain.entity.Address;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.infrastructure.persistence.AddressJpaRepository;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.INTERNAL_USERS)
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;
    private final AddressJpaRepository addressJpaRepository;

    @GetMapping("/{userId}")
    public ApiResponse<UserDto> getUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        return ApiResponse.ok(new UserDto(
                user.getId(), user.getEmail(), user.getFullName(),
                user.isActive(), user.getRole().name()));
    }

    @GetMapping("/{userId}/default-address")
    public ApiResponse<AddressDto> getDefaultAddress(@PathVariable UUID userId) {
        List<Address> defaults = addressJpaRepository.findByUserIdAndIsDefaultTrue(userId);
        if (defaults.isEmpty())
            throw new BusinessException("No default address for user: " + userId);
        Address addr = defaults.get(0);
        return ApiResponse.ok(new AddressDto(
                addr.getId(), addr.getFullName(), addr.getPhone(),
                addr.getStreet(), addr.getDistrict(), addr.getCity()));
    }

    @GetMapping("/{userId}/contact")
    public ApiResponse<UserContactDto> getUserContact(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        return ApiResponse.ok(new UserContactDto(user.getId(), user.getEmail(), user.getFullName()));
    }

    record UserDto(UUID id, String email, String fullName, boolean active, String role) {}
    record AddressDto(UUID id, String fullName, String phone,
                       String street, String district, String city) {}
    record UserContactDto(UUID id, String email, String fullName) {}
}
