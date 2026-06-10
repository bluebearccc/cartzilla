package com.cartzilla.order.domain.entity;

import com.cartzilla.order.domain.vo.SagaStatus;
import com.cartzilla.order.domain.vo.SagaStep;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Aggregate root — OrderSagaAggregate (1:1 Order).
 * Rules: SA-01..SA-06, BR-O12..BR-O13.
 * UNIQUE order_id — SA-01.
 */
@Entity
@Table(name = "saga_states")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaState extends BaseEntity {

    private static final int MAX_RETRY = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** SA-01: UNIQUE */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /** SA-02 */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 20)
    private SagaStep currentStep;

    /** SA-03 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SagaStatus status;

    /** SA-05: ≥ 0 */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** SS-01: khởi tạo RESERVE_STOCK / IN_PROGRESS / retryCount=0 */
    public static SagaState start(UUID orderId) {
        if (orderId == null) throw new BusinessException("orderId must not be null (SA-01)");
        SagaState s = new SagaState();
        s.orderId = orderId;
        s.currentStep = SagaStep.RESERVE_STOCK;
        s.status = SagaStatus.IN_PROGRESS;
        s.retryCount = 0;
        return s;
    }

    /** SA-04: tiến step — không được nhảy bước */
    public void advanceStep() {
        if (status != SagaStatus.IN_PROGRESS)
            throw new BusinessException("Saga is not IN_PROGRESS");
        this.currentStep = switch (currentStep) {
            case RESERVE_STOCK -> SagaStep.PROCESS_PAYMENT;
            case PROCESS_PAYMENT -> SagaStep.NOTIFY;
            case NOTIFY -> SagaStep.DONE;
            case DONE -> throw new BusinessException("Saga already at DONE step (SA-04)");
        };
        if (currentStep == SagaStep.DONE) this.status = SagaStatus.COMPLETED;
    }

    /** Hoàn tất saga: tiến tuần tự tới DONE (SA-04 — không nhảy bước) → COMPLETED */
    public void complete() {
        if (status != SagaStatus.IN_PROGRESS)
            throw new BusinessException("Saga is not IN_PROGRESS");
        while (currentStep != SagaStep.DONE) advanceStep();
    }

    /** SA-05/BR-O13: tăng retry; nếu vượt ngưỡng → FAILED */
    public void recordRetry(String error) {
        this.retryCount++;
        this.errorMessage = error;
        if (retryCount >= MAX_RETRY) {
            this.status = SagaStatus.FAILED;
        }
    }

    public void fail(String error) {
        this.status = SagaStatus.FAILED;
        this.errorMessage = error;
    }
}
