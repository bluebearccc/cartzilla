package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.VoucherCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.entity.Voucher;
import com.cartzilla.user.domain.entity.VoucherUsage;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.domain.repository.VoucherAllowedUserRepository;
import com.cartzilla.user.domain.exception.UnprocessableEntityException;
import com.cartzilla.user.domain.repository.VoucherRepository;
import com.cartzilla.user.domain.repository.VoucherUsageRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedeemVoucherUseCase {

    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherUsageRepository usageRepository;
    private final VoucherAllowedUserRepository allowedUserRepository;

    public record Result(UUID usageId, String code, BigDecimal discountAmount, boolean idempotent) {}

    @Transactional
    public Result execute(VoucherCommand.Redeem command) {
        if (command.orderId() == null) {
            throw new BusinessException("orderId is required");
        }
        Voucher voucher = voucherRepository.findByCode(requireCode(command.code()))
                .orElseThrow(() -> new BusinessException("Voucher not found: " + command.code()));
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new BusinessException("User not found: " + command.userId()));
        user.requireActive();

        VoucherUsage existingUsage = usageRepository.findByVoucherIdAndOrderId(voucher.getId(), command.orderId())
                .orElse(null);
        if (existingUsage != null) {
            return new Result(existingUsage.getId(), voucher.getCode(), existingUsage.getDiscountAmount(), true);
        }

        ValidateVoucherUseCase validator = new ValidateVoucherUseCase(
                voucherRepository, userRepository, usageRepository, allowedUserRepository);
        validator.validateEligibility(voucher, user, command.orderSubtotal());
        BigDecimal discount = validator.calculateDiscount(voucher, command.orderSubtotal());

        // BR-V05: ghi VoucherUsage trước (unique (voucherId, orderId) đảm bảo idempotent),
        // chỉ tăng usedCount khi tạo được usage mới. Nếu tăng count không thành công (hết lượt)
        // thì throw → toàn bộ transaction rollback, không over-count, không để usage mồ côi.
        VoucherUsage usage = usageRepository.save(VoucherUsage.redeem(
                voucher.getId(), user.getId(), command.orderId(), discount));
        if (voucherRepository.incrementUsedCountIfAvailable(voucher.getId()) != 1) {
            throw new UnprocessableEntityException("Voucher has reached max uses (VA-02)");
        }
        return new Result(usage.getId(), voucher.getCode(), discount, false);
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Voucher code is required");
        }
        return code.trim();
    }
}
