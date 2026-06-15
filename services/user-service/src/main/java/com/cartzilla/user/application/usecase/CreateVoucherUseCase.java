package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateVoucherUseCase {

    private final VoucherRepository voucherRepository;

    @Transactional
    public Voucher execute(VoucherCommand.Create command) {
        if (voucherRepository.existsByCode(command.code())) {
            throw new BusinessException("Voucher code already exists: " + command.code());
        }
        Voucher voucher = Voucher.create(
                command.code(),
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
                command.minTotalSpent());
        return voucherRepository.save(voucher);
    }
}
