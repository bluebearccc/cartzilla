package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.UserSearchCriteria;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.user.infrastructure.persistence.UserJpaRepository;
import com.cartzilla.user.infrastructure.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    @Override public User save(User user) { return jpa.save(user); }
    @Override public Optional<User> findById(UUID id) { return jpa.findById(id); }
    @Override public Optional<User> findByEmail(String email) { return jpa.findByEmail(email); }
    @Override public boolean existsByEmail(String email) { return jpa.existsByEmail(email); }
    @Override public Optional<User> findByPhone(String phone) { return jpa.findByPhone(phone); }
    @Override public Page<User> search(UserSearchCriteria criteria, Pageable pageable) {
        return jpa.findAll(UserSpecifications.from(criteria), pageable);
    }
    @Override public long countActiveAdminsExcluding(UUID excludedUserId) {
        return jpa.countByRoleAndActiveTrueAndIdNot(Role.ADMIN, excludedUserId);
    }
}
