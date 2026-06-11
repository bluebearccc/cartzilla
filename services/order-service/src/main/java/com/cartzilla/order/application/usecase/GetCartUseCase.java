package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.order.domain.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCartUseCase {

    private final CartItemRepository cartItemRepository;

    @Transactional(readOnly = true)
    public List<CartItem> execute(UUID userId) {
        return cartItemRepository.findByUserId(userId);
    }
}
