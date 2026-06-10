package com.cartzilla.product.domain.vo;

import com.cartzilla.web.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** VO: Money — BR-G05: amount ≥ 0, scale 2, VND. */
public final class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("Money amount must be >= 0 (BR-G05)");
        return new Money(amount);
    }

    public BigDecimal getAmount() { return amount; }

    @Override public boolean equals(Object o) {
        return o instanceof Money m && amount.compareTo(m.amount) == 0;
    }
    @Override public int hashCode() { return Objects.hash(amount.stripTrailingZeros()); }
    @Override public String toString() { return amount + " VND"; }
}
