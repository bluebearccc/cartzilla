package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.infrastructure.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** ADAPTER — implement domain port bằng Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    @Override public User save(User user) { return jpa.save(user); }
    @Override public Optional<User> findById(UUID id) { return jpa.findById(id); }
    @Override public Optional<User> findByEmail(String email) { return jpa.findByEmail(email); }
    @Override public boolean existsByEmail(String email) { return jpa.existsByEmail(email); }
}
