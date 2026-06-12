package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.Address;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.AddressRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListAddressesUseCase {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public List<Address> execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();
        return addressRepository.findByUserId(userId);
    }
}
