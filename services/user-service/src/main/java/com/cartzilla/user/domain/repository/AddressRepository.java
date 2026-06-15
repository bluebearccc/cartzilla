package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.Address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {
    Address save(Address address);
    Optional<Address> findById(UUID id);
    List<Address> findByUserId(UUID userId);
    List<Address> findDefaultByUserId(UUID userId);
    void delete(Address address);
}
