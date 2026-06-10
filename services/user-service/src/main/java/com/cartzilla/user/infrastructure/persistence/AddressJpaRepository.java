package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressJpaRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserId(UUID userId);

    /** UA-05: dùng khi cần unset existing default trước khi set default mới */
    List<Address> findByUserIdAndIsDefaultTrue(UUID userId);
}
