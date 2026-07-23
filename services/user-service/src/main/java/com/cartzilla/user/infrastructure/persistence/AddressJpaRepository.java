package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressJpaRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserId(UUID userId);

    List<Address> findByUserIdAndIsDefaultTrue(UUID userId);
}
