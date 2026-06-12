package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.Address;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.AddressRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteAddressUseCase {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public void execute(UUID userId, UUID addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("Address not found: " + addressId));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("Address not found: " + addressId);
        }

        if (address.isDefault() && addressRepository.findByUserId(userId).size() > 1) {
            throw new BusinessException("Cannot delete default address while other addresses exist. Set another default address first.");
        }

        addressRepository.delete(address);
    }
}
