CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    external_payment_id VARCHAR(255),
    preference_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_method VARCHAR(100),
    payer_email VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    webhook_received_at TIMESTAMP,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_external_id ON payments(external_payment_id);
CREATE INDEX idx_payments_preference_id ON payments(preference_id);
