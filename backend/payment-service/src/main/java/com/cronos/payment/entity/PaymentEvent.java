package com.cronos.payment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_events")
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "provider_payment_id", nullable = false, length = 255)
    private String providerPaymentId;

    @Column(name = "provider_status", nullable = false, length = 40)
    private String providerStatus;

    @Column(name = "provider_status_detail", length = 100)
    private String providerStatusDetail;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PaymentEvent() {}

    public PaymentEvent(UUID paymentId, String providerPaymentId, String providerStatus, String providerStatusDetail,
                        BigDecimal amount, String currency, String source) {
        this.paymentId = paymentId;
        this.providerPaymentId = providerPaymentId;
        this.providerStatus = providerStatus;
        this.providerStatusDetail = providerStatusDetail;
        this.amount = amount;
        this.currency = currency;
        this.source = source;
        this.createdAt = LocalDateTime.now();
    }

    public String getProviderPaymentId() { return providerPaymentId; }
    public String getProviderStatus() { return providerStatus; }
    public String getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
