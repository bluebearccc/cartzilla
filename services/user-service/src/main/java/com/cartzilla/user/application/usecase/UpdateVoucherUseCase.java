package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateVoucherUseCase {

    private final VoucherRepository voucherRepository;

    @Transactional
    public Voucher execute(UUID id, VoucherCommand.Update command) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Voucher not found: " + id));
        voucher.updateRules(
                command.discountType(),
                command.discountValue(),
                command.maxDiscountAmount(),
                command.minOrderAmount(),
                command.maxUses(),
                command.startsAt(),
                command.expiresAt(),
                command.minAccountAgeDays(),
                command.perUserLimit(),
                command.audienceType(),
                command.firstOrderOnly(),
                command.minCompletedOrders(),
                command.minTotalSpent(),
                command.active());
        return voucherRepository.save(voucher);
    }
}
