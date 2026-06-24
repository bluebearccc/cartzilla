package com.cartzilla.user.infrastructure.persistence;

import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.vo.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByPhone(String phone);
    long countByRoleAndActiveTrueAndIdNot(Role role, UUID excludedUserId);
}
