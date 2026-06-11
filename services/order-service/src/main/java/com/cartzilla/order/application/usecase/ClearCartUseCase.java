package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClearCartUseCase {

    private final CartItemRepository cartItemRepository;

    @Transactional
    public void execute(UUID userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
