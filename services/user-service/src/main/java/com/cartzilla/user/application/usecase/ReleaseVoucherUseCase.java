package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.entity.VoucherUsage;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.user.domain.repository.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseVoucherUseCase {
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository usageRepository;

    @Transactional
    public boolean execute(String code, UUID userId, UUID orderId) {
        Voucher voucher = voucherRepository.findByCode(code).orElse(null);
        if (voucher == null) return true; 
        VoucherUsage usage = usageRepository.findByVoucherIdAndOrderId(voucher.getId(), orderId).orElse(null);
        if (usage == null || usage.isReleased()) return true;
        if (!usage.getUserId().equals(userId)) return false;
        if (voucherRepository.decrementUsedCountIfPositive(voucher.getId()) != 1) return false;
        usage.release();
        usageRepository.save(usage);
        return true;
    }
}
