package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteVoucherUseCase {

    private final VoucherRepository voucherRepository;

    @Transactional
    public void execute(UUID id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Voucher not found: " + id));
        voucher.deactivate();
        voucher.softDelete();
        voucherRepository.save(voucher);
    }
}
