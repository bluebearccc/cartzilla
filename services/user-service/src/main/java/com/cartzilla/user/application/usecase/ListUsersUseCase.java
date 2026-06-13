package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AdminUserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.UserSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<User> execute(AdminUserCommand.Search command, int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        UserSearchCriteria criteria = new UserSearchCriteria(command.keyword(), command.role(), command.active());
        return userRepository.search(criteria, PageRequest.of(safePage, safeSize, mapSort(sort)));
    }

    private Sort mapSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("email").ascending();
        }
        String[] parts = sort.split(",", 2);
        String field = switch (parts[0]) {
            case "fullName" -> "fullName";
            case "role" -> "role";
            case "active" -> "active";
            case "createdAt" -> "createdAt";
            default -> "email";
        };
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
