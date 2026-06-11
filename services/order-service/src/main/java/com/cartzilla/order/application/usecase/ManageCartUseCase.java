package com.cartzilla.order.application.usecase;

import com.cartzilla.order.application.command.CartCommand;
import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.order.domain.repository.CartRepository;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * F05 — UC-03: quản lý giỏ hàng server-side theo user.
 * Rules: CTA-01 (unique user+sku, cộng dồn), CTA-05 (snapshot từ product-service),
 * CI-01/CI-02 (qty>0, price>=0), BR-O01/BR-O02.
 */
@Service
@RequiredArgsConstructor
public class ManageCartUseCase {

    private final CartRepository cartRepository;
    private final ProductFeignClient productFeignClient;

    /** Thêm vào giỏ — nếu SKU đã có thì cộng quantity (BR-O01), không tạo dòng trùng. */
    @Transactional
    public CartItem addItem(CartCommand.AddItem cmd) {
        if (cmd.userId() == null) throw new BusinessException("userId is required");
        if (cmd.quantity() <= 0) throw new BusinessException("quantity must be > 0 (CI-01)");
        String sku = requireText(cmd.sku(), "sku").toUpperCase();

        var existing = cartRepository.findByUserIdAndSku(cmd.userId(), sku);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + cmd.quantity();
            assertStockAvailable(sku, newQty); // không cho cộng vượt tồn kho
            item.addQuantity(cmd.quantity());  // CTA-01/BR-O01
            return cartRepository.save(item);
        }

        var variant = fetchVariant(sku);
        if (!variant.active()) throw new BusinessException("Variant is inactive: " + sku);
        if (variant.stock() < cmd.quantity())
            throw new BusinessException("Insufficient stock for sku=" + sku
                    + ". available=" + variant.stock() + ", requested=" + cmd.quantity());

        // CTA-05: snapshot product data tại thời điểm thêm vào giỏ
        CartItem item = CartItem.create(cmd.userId(), variant.productId(), variant.sku(),
                variant.productName(), variant.primaryImage(), variant.size(), variant.color(),
                variant.price(), cmd.quantity());
        return cartRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<CartItem> view(UUID userId) {
        if (userId == null) throw new BusinessException("userId is required");
        return cartRepository.findByUserId(userId);
    }

    /** Cập nhật quantity; quantity = 0 → xóa dòng cart (BR-O02). */
    @Transactional
    public void updateQuantity(CartCommand.UpdateItem cmd) {
        CartItem item = cartRepository.findByIdAndUserId(cmd.itemId(), cmd.userId())
                .orElseThrow(() -> new NoSuchElementException("Cart item not found: " + cmd.itemId()));
        if (cmd.quantity() <= 0) {           // BR-O02
            cartRepository.remove(item);
            return;
        }
        assertStockAvailable(item.getSku(), cmd.quantity());
        item.updateQuantity(cmd.quantity());
        cartRepository.save(item);
    }

    @Transactional
    public void removeItem(UUID userId, UUID itemId) {
        CartItem item = cartRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NoSuchElementException("Cart item not found: " + itemId));
        cartRepository.remove(item);
    }

    private void assertStockAvailable(String sku, int requestedQty) {
        var variant = fetchVariant(sku);
        if (!variant.active()) throw new BusinessException("Variant is inactive: " + sku);
        if (variant.stock() < requestedQty)
            throw new BusinessException("Insufficient stock for sku=" + sku
                    + ". available=" + variant.stock() + ", requested=" + requestedQty);
    }

    private ProductFeignClient.VariantSnapshotDto fetchVariant(String sku) {
        var response = productFeignClient.getVariantBySku(sku);
        var variant = response == null ? null : response.data();
        if (response == null || !response.success() || variant == null)
            throw new BusinessException("Cannot load product variant for sku=" + sku);
        return variant;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new BusinessException(field + " is required");
        return value.trim();
    }
}
