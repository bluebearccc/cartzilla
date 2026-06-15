package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.VoucherAllowedUser;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddVoucherAllowedUserUseCase {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherAllowedUserRepository allowedUserRepository;

    @Transactional
    public VoucherAllowedUser execute(UUID voucherId, UUID userId) {
        voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("Voucher not found: " + voucherId));
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        if (allowedUserRepository.existsByVoucherIdAndUserId(voucherId, userId)) {
            throw new BusinessException("User is already allowed for this voucher");
        }
        return allowedUserRepository.save(VoucherAllowedUser.add(voucherId, userId));
    }
}
