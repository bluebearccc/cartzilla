package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.exception.ForbiddenException;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnsureAdminUseCase {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void execute(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Admin account not found"));
        if (!user.isActive() || user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Admin role required");
        }
    }
}
