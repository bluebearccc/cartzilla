package com.cartzilla.product.application.usecase;

import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.vo.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Trừ tồn kho khi nhận stock.reserve; trả về SKU lỗi nếu không đủ. */
@Service
@RequiredArgsConstructor
public class ReserveStockUseCase {

    private final ProductRepository productRepository;

    /** @return null nếu thành công, ngược lại trả SKU bị thiếu. */
    public String reserve(StockEvents.StockReserveEvent event) {
        for (StockEvents.Item item : event.items()) {
            Product product = findBySku(item.sku());
            if (product == null) return item.sku();
            ProductVariant v = product.getVariants().stream()
                    .filter(x -> x.getSku().equals(item.sku())).findFirst().orElse(null);
            if (v == null || v.getStock() < item.quantity()) return item.sku();
        }
        // đủ hàng → trừ kho
        for (StockEvents.Item item : event.items()) {
            Product product = findBySku(item.sku());
            product.getVariants().stream()
                    .filter(x -> x.getSku().equals(item.sku())).findFirst()
                    .ifPresent(v -> v.setStock(v.getStock() - item.quantity()));
            productRepository.save(product);
        }
        return null;
    }

    public void release(StockEvents.StockReleaseEvent event) {
        for (StockEvents.Item item : event.items()) {
            Product product = findBySku(item.sku());
            if (product == null) continue;
            product.getVariants().stream()
                    .filter(x -> x.getSku().equals(item.sku())).findFirst()
                    .ifPresent(v -> v.setStock(v.getStock() + item.quantity()));
            productRepository.save(product);
        }
    }

    private Product findBySku(String sku) {
        return productRepository.findAllActive().stream()
                .filter(p -> p.getVariants().stream().anyMatch(v -> v.getSku().equals(sku)))
                .findFirst().orElse(null);
    }
}
