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
public class SetDefaultAddressUseCase {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public Address execute(UUID userId, UUID addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();
        Address selected = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("Address not found: " + addressId));
        if (!selected.getUserId().equals(userId)) {
            throw new BusinessException("Address not found: " + addressId);
        }

        for (Address address : addressRepository.findDefaultByUserId(userId)) {
            if (!address.getId().equals(selected.getId())) {
                address.unsetDefault();
                addressRepository.save(address);
            }
        }
        selected.setAsDefault();
        return addressRepository.save(selected);
    }
}
