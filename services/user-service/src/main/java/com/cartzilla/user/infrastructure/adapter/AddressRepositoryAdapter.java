package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.Address;
import com.cartzilla.user.domain.repository.AddressRepository;
import com.cartzilla.user.infrastructure.persistence.AddressJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AddressRepositoryAdapter implements AddressRepository {

    private final AddressJpaRepository jpa;

    @Override
    public Address save(Address address) {
        return jpa.save(address);
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Address> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public List<Address> findDefaultByUserId(UUID userId) {
        return jpa.findByUserIdAndIsDefaultTrue(userId);
    }

    @Override
    public void delete(Address address) {
        address.softDelete();
        jpa.save(address);
    }
}
