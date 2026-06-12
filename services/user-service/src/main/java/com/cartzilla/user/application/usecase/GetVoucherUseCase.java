package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetVoucherUseCase {

    private final VoucherRepository voucherRepository;

    public Voucher execute(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Voucher not found: " + id));
    }
}
