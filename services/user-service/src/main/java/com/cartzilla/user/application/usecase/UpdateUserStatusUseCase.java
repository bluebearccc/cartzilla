package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AdminUserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateUserStatusUseCase {
    private final UserRepository userRepository;

    @Transactional
    public User execute(UUID userId, AdminUserCommand.UpdateStatus command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        if (!command.active() && user.getRole() == Role.ADMIN) {
            ensureAnotherActiveAdminExists(user.getId());
        }
        if (command.active()) {
            user.activate();
        } else {
            user.deactivate();
        }
        return userRepository.save(user);
    }

    private void ensureAnotherActiveAdminExists(UUID userId) {
        if (userRepository.countActiveAdminsExcluding(userId) == 0) {
            throw new BusinessException("Cannot deactivate the last admin");
        }
    }
}
