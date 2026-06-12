package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveVoucherAllowedUserUseCase {

    private final VoucherRepository voucherRepository;
    private final VoucherAllowedUserRepository allowedUserRepository;

    @Transactional
    public void execute(UUID voucherId, UUID userId) {
        voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("Voucher not found: " + voucherId));
        allowedUserRepository.deleteByVoucherIdAndUserId(voucherId, userId);
    }
}
