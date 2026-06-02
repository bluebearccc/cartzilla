package com.cartzilla.order.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "saga_states")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaState extends BaseEntity {

    public enum Step { RESERVE_STOCK, PROCESS_PAYMENT, NOTIFY, DONE }
    public enum Status { IN_PROGRESS, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step")
    private Step currentStep;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    public static SagaState start(UUID orderId) {
        SagaState s = new SagaState();
        s.orderId = orderId;
        s.currentStep = Step.RESERVE_STOCK;
        s.status = Status.IN_PROGRESS;
        return s;
    }

    public void moveTo(Step step) { this.currentStep = step; }
    public void complete() { this.currentStep = Step.DONE; this.status = Status.COMPLETED; }
    public void fail(String reason) { this.status = Status.FAILED; this.errorMessage = reason; }
}
