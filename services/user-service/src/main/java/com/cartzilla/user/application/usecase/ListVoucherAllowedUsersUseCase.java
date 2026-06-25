package com.cartzilla.user.application.usecase;

import com.cartzilla.user.api.dto.VoucherDtos.AllowedUserResponse;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListVoucherAllowedUsersUseCase {

    private final VoucherRepository voucherRepository;
    private final VoucherAllowedUserRepository allowedUserRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AllowedUserResponse> execute(UUID voucherId) {
        voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("Voucher not found: " + voucherId));
        return allowedUserRepository.findByVoucherId(voucherId).stream()
                .map(vau -> {
                    User u = userRepository.findById(vau.getUserId()).orElse(null);
                    String email = u != null ? u.getEmail() : "";
                    String fullName = u != null ? u.getFullName() : "";
                    return AllowedUserResponse.from(vau, email, fullName);
                })
                .toList();
    }
}
