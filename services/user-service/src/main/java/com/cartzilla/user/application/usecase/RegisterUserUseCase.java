package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AuthCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.exception.DuplicateEmailException;
import com.cartzilla.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UUID execute(AuthCommand.Register cmd) {
        if (userRepository.existsByEmail(cmd.email())) {
            throw new DuplicateEmailException(cmd.email());
        }
        User user = User.createCustomer(
                cmd.email(),
                passwordEncoder.encode(cmd.password()),
                cmd.fullName());
        return userRepository.save(user).getId();
    }
}
