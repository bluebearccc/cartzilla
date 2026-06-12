package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AddressCommand;
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
public class UpdateAddressUseCase {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public Address execute(UUID userId, UUID addressId, AddressCommand.Update command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();
        Address address = findOwnedAddress(userId, addressId);
        address.updateDetails(command.fullName(), command.phone(), command.street(), command.district(), command.city());

        if (Boolean.TRUE.equals(command.defaultAddress())) {
            setAsOnlyDefault(userId, address);
        }

        return addressRepository.save(address);
    }

    private Address findOwnedAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException("Address not found: " + addressId));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("Address not found: " + addressId);
        }
        return address;
    }

    private void setAsOnlyDefault(UUID userId, Address selected) {
        for (Address address : addressRepository.findDefaultByUserId(userId)) {
            if (!address.getId().equals(selected.getId())) {
                address.unsetDefault();
                addressRepository.save(address);
            }
        }
        selected.setAsDefault();
    }
}
