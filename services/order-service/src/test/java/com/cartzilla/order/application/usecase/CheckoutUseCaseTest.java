package com.cartzilla.order.application.usecase;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.domain.repository.CartRepository;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.order.infrastructure.feign.ProductFeignClient;
import com.cartzilla.order.infrastructure.feign.UserFeignClient;
import com.cartzilla.order.infrastructure.saga.OrderSagaOrchestrator;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckoutUseCaseTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CartRepository cartRepository = mock(CartRepository.class);
    private final OrderSagaOrchestrator sagaOrchestrator = mock(OrderSagaOrchestrator.class);
    private final ProductFeignClient productFeignClient = mock(ProductFeignClient.class);
    private final UserFeignClient userFeignClient = mock(UserFeignClient.class);
    private final CheckoutUseCase useCase = new CheckoutUseCase(
            orderRepository,
            cartRepository,
            sagaOrchestrator,
            productFeignClient,
            userFeignClient,
            new ObjectMapper());

    @Test
    void executeFromSkus_rejectsInsufficientStock() {
        String sku = "TSN-001-M-WHT";
        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(
                new ProductFeignClient.VariantSnapshotDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        sku,
                        "Demo shirt",
                        null,
                        "M",
                        "White",
                        new BigDecimal("199000"),
                        1,
                        true)));

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.executeFromSkus(
                new OrderCommand.CheckoutBySku(
                        UUID.randomUUID(),
                        List.of(new OrderCommand.SkuLine(sku, 2)),
                        "{\"fullName\":\"Demo\",\"phone\":\"0900000000\",\"city\":\"HCM\"}",
                        PaymentMethod.COD,
                        null)));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        verifyNoInteractions(orderRepository, sagaOrchestrator);
    }

    @Test
    void executeFromSkus_failsClosedWhenVoucherServiceErrors_BRV12() {
        String sku = "TSN-001-M-WHT";
        when(productFeignClient.getVariantBySku(sku)).thenReturn(ApiResponse.ok(
                new ProductFeignClient.VariantSnapshotDto(
                        UUID.randomUUID(), UUID.randomUUID(), sku, "Demo shirt", null,
                        "M", "White", new BigDecimal("199000"), 10, true)));
        when(userFeignClient.validateVoucher(anyString(), any(), any()))
                .thenThrow(new RuntimeException("voucher service down"));

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.executeFromSkus(
                new OrderCommand.CheckoutBySku(
                        UUID.randomUUID(),
                        List.of(new OrderCommand.SkuLine(sku, 1)),
                        "{\"fullName\":\"Demo\",\"phone\":\"0900000000\",\"city\":\"HCM\"}",
                        PaymentMethod.COD,
                        "SALE10")));

        assertTrue(ex.getMessage().toLowerCase().contains("voucher"));
        verifyNoInteractions(orderRepository, sagaOrchestrator);
    }
}
