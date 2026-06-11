package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.order.domain.repository.CartItemRepository;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient.VariantSnapshotDto;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateCartItemUseCase {

    private final CartItemRepository cartItemRepository;
    private final ProductFeignClient productFeignClient;   // ← OpenFeign

    @Transactional
    public CartItem execute(UUID userId, String sku, int newQuantity) {

        CartItem item = cartItemRepository.findByUserIdAndSku(userId, sku.toUpperCase())
                .orElseThrow(() -> new BusinessException("Cart item not found: " + sku));

        // quantity = 0 → hard-delete (CTA-02)
        if (newQuantity == 0) {
            cartItemRepository.deleteByUserIdAndSku(userId, sku.toUpperCase());
            return null;
        }

        // Gọi product-service validate stock mới nhất
        VariantSnapshotDto variant = productFeignClient
                .getVariantBySku(sku.toUpperCase())
                .data();

        if (!variant.active()) {
            throw new BusinessException("Sản phẩm không còn khả dụng: " + sku);
        }
        if (variant.stock() < newQuantity) {
            throw new BusinessException(
                    "Không đủ tồn kho (còn: " + variant.stock() +
                    ", yêu cầu: " + newQuantity + ")");
        }

        item.updateQuantity(newQuantity);
        item.refreshPrice(variant.price());   // refresh snapshot price
        return cartItemRepository.save(item);
    }
}
