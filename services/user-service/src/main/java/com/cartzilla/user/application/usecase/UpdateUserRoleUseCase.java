package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AdminUserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.vo.Role;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateUserRoleUseCase {
    private final UserRepository userRepository;

    @Transactional
    public User execute(UUID userId, AdminUserCommand.UpdateRole command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        Role targetRole = command.role();
        if (targetRole == null) {
            throw new BusinessException("role must not be null");
        }
        if (user.getRole() == Role.ADMIN && targetRole != Role.ADMIN) {
            ensureAnotherActiveAdminExists(user.getId());
        }
        user.changeRole(targetRole);
        return userRepository.save(user);
    }

    private void ensureAnotherActiveAdminExists(UUID userId) {
        if (userRepository.countActiveAdminsExcluding(userId) == 0) {
            throw new BusinessException("Cannot change role of the last admin");
        }
    }
}
