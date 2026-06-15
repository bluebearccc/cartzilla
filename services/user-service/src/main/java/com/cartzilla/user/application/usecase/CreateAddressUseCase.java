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
public class CreateAddressUseCase {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public Address execute(UUID userId, AddressCommand.Create command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();

        boolean makeDefault = command.defaultAddress() || addressRepository.findByUserId(userId).isEmpty();
        if (makeDefault) {
            unsetExistingDefaults(userId);
        }

        Address address = Address.create(
                userId,
                command.fullName(),
                command.phone(),
                command.street(),
                command.district(),
                command.city(),
                makeDefault);
        return addressRepository.save(address);
    }

    private void unsetExistingDefaults(UUID userId) {
        for (Address address : addressRepository.findDefaultByUserId(userId)) {
            address.unsetDefault();
            addressRepository.save(address);
        }
    }
}
