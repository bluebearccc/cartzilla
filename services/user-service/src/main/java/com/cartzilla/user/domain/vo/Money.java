package com.cartzilla.user.domain.vo;

import com.cartzilla.web.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** VO: Money — BR-G05: amount ≥ 0, scale 2, currency VND. */
public final class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO);
    public static final String DEFAULT_CURRENCY = "VND";

    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = DEFAULT_CURRENCY;
    }

    public static Money of(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("Money amount must be >= 0, got: " + amount);
        return new Money(amount);
    }

    public static Money of(long amount) { return of(BigDecimal.valueOf(amount)); }

    public Money add(Money other) { return new Money(this.amount.add(other.amount)); }

    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("Money subtraction result is negative");
        return new Money(result);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }

    @Override public boolean equals(Object o) {
        return o instanceof Money m && amount.compareTo(m.amount) == 0;
    }
    @Override public int hashCode() { return Objects.hash(amount.stripTrailingZeros()); }
    @Override public String toString() { return amount + " " + currency; }
}
