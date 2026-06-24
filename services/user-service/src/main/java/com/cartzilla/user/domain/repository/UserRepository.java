package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** PORT — domain định nghĩa, infrastructure implement. */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByPhone(String phone);
    Page<User> search(UserSearchCriteria criteria, Pageable pageable);
    long countActiveAdminsExcluding(UUID excludedUserId);
}

