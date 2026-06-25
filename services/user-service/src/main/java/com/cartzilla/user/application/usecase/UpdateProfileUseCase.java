package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.UserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final UserRepository userRepository;

    @Transactional
    public User execute(UUID userId, UserCommand.UpdateProfile command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();

        String newPhone = command.phone();
        if (newPhone != null && !newPhone.isBlank()) {
            String trimmedPhone = newPhone.trim();
            userRepository.findByPhone(trimmedPhone).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(userId)) {
                    throw new BusinessException("Số điện thoại đã được sử dụng");
                }
            });
        }

        user.updateProfile(command.fullName(), command.phone());
        return userRepository.save(user);
    }
}
