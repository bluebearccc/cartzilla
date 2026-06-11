package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.order.domain.repository.CartItemRepository;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient.VariantSnapshotDto;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCartItemUseCaseTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductFeignClient productFeignClient;

    @InjectMocks
    private UpdateCartItemUseCase updateCartItemUseCase;

    private UUID userId;
    private String sku;
    private CartItem existingItem;
    private VariantSnapshotDto activeVariant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sku = "TSN-001-S-WHT";
        existingItem = CartItem.create(
                userId, UUID.randomUUID(), sku, "Áo thun nam Basic Cotton",
                "http://img.jpg", "S", "Trắng", new BigDecimal("199000"), 3
        );
        activeVariant = new VariantSnapshotDto(
                existingItem.getProductId(),
                UUID.randomUUID(),
                sku,
                "Áo thun nam Basic Cotton",
                "http://img.jpg",
                "S",
                "Trắng",
                new BigDecimal("199000"),
                10, // stock
                true // active
        );
    }

    @Test
    void execute_updateToZeroQuantity_deletesItem_returnsNull() {
        when(cartItemRepository.findByUserIdAndSku(userId, sku)).thenReturn(Optional.of(existingItem));

        CartItem result = updateCartItemUseCase.execute(userId, sku, 0);

        assertNull(result);
        verify(cartItemRepository).deleteByUserIdAndSku(userId, sku);
        verify(productFeignClient, never()).getVariantBySku(any());
    }

    @Test
    void execute_updateToValidQuantity_success() {
        when(cartItemRepository.findByUserIdAndSku(userId, sku)).thenReturn(Optional.of(existingItem));
        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(activeVariant));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItem result = updateCartItemUseCase.execute(userId, sku, 5);

        assertNotNull(result);
        assertEquals(5, result.getQuantity());
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void execute_itemNotFound_throwsException() {
        when(cartItemRepository.findByUserIdAndSku(userId, sku)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> updateCartItemUseCase.execute(userId, sku, 5));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void execute_stockInsufficient_throwsException() {
        when(cartItemRepository.findByUserIdAndSku(userId, sku)).thenReturn(Optional.of(existingItem));
        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(activeVariant));

        assertThrows(BusinessException.class, () -> updateCartItemUseCase.execute(userId, sku, 15)); // stock is 10
        verify(cartItemRepository, never()).save(any());
    }
}
