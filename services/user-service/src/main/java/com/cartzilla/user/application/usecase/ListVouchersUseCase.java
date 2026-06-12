package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListVouchersUseCase {

    private final VoucherRepository voucherRepository;

    public List<Voucher> execute() {
        return voucherRepository.findAll();
    }
}
