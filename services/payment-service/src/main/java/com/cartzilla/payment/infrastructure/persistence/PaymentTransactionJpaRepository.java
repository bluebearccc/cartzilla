package com.cartzilla.payment.infrastructure.persistence;

import com.cartzilla.payment.domain.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransaction, UUID> {
    boolean existsByProviderAndProviderTxnRef(String provider, String providerTxnRef);
}
