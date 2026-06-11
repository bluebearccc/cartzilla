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
class AddToCartUseCaseTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductFeignClient productFeignClient;

    @InjectMocks
    private AddToCartUseCase addToCartUseCase;

    private UUID userId;
    private String sku;
    private VariantSnapshotDto activeVariant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sku = "TSN-001-S-WHT";
        activeVariant = new VariantSnapshotDto(
                UUID.randomUUID(),
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
    void execute_createNewCartItem_success() {
        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(activeVariant));
        when(cartItemRepository.findByUserIdAndSku(userId, sku)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItem result = addToCartUseCase.execute(userId, sku, 2);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(sku, result.getSku());
        assertEquals(2, result.getQuantity());
        assertEquals(activeVariant.price(), result.getPrice());
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void execute_variantInactive_throwsException() {
        VariantSnapshotDto inactive = new VariantSnapshotDto(
                activeVariant.productId(), activeVariant.variantId(), sku,
                activeVariant.productName(), activeVariant.primaryImage(),
                activeVariant.size(), activeVariant.color(), activeVariant.price(),
                10, false
        );
        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(inactive));

        assertThrows(BusinessException.class, () -> addToCartUseCase.execute(userId, sku, 2));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void execute_insufficientStock_throwsException() {
        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(activeVariant));

        assertThrows(BusinessException.class, () -> addToCartUseCase.execute(userId, sku, 15));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void execute_itemAlreadyInCart_aggregatesQuantity_success() {
        CartItem existingItem = CartItem.create(
                userId, activeVariant.productId(), sku, activeVariant.productName(),
                activeVariant.primaryImage(), activeVariant.size(), activeVariant.color(),
                activeVariant.price(), 3
        );

        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(activeVariant));
        when(cartItemRepository.findByUserIdAndSku(userId, sku)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartItem result = addToCartUseCase.execute(userId, sku, 4);

        assertNotNull(result);
        assertEquals(7, result.getQuantity()); // 3 + 4
        verify(cartItemRepository).save(existingItem);
    }
}
