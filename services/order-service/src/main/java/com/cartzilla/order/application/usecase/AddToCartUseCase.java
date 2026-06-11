package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.order.domain.repository.CartItemRepository;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient.VariantSnapshotDto;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddToCartUseCase {

    private final CartItemRepository cartItemRepository;
    private final ProductFeignClient productFeignClient;   // ← OpenFeign

    @Transactional
    public CartItem execute(UUID userId, String sku, int quantity) {

        // ① Gọi product-service qua OpenFeign lấy variant snapshot
        VariantSnapshotDto variant = productFeignClient
                .getVariantBySku(sku.toUpperCase())
                .data();

        // ② Validate: variant phải active (CTA-03)
        if (!variant.active()) {
            throw new BusinessException("Sản phẩm không khả dụng: " + sku);
        }

        // ③ Validate: tồn kho đủ (X-01)
        if (variant.stock() < quantity) {
            throw new BusinessException(
                    "Không đủ tồn kho cho SKU " + sku +
                    " (còn: " + variant.stock() + ", yêu cầu: " + quantity + ")");
        }

        // ④ Kiểm tra đã có trong giỏ chưa → cộng dồn quantity (CTA-01)
        Optional<CartItem> existing = cartItemRepository
                .findByUserIdAndSku(userId, sku.toUpperCase());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            // Validate tổng quantity mới vẫn <= stock
            int newTotal = item.getQuantity() + quantity;
            if (variant.stock() < newTotal) {
                throw new BusinessException(
                        "Tổng số lượng vượt tồn kho (đang có: " +
                        item.getQuantity() + ", thêm: " + quantity +
                        ", kho: " + variant.stock() + ")");
            }
            item.addQuantity(quantity);
            // Cập nhật lại snapshot price (giá có thể thay đổi) — CTA-05
            item.refreshPrice(variant.price());
            return cartItemRepository.save(item);
        }

        // ⑤ Tạo mới CartItem với snapshot từ product-service
        CartItem newItem = CartItem.create(
                userId,
                variant.productId(),
                variant.sku(),
                variant.productName(),
                variant.primaryImage(),
                variant.size(),
                variant.color(),
                variant.price(),
                quantity
        );
        return cartItemRepository.save(newItem);
    }
}
