-- One payment record per order; every provider notification is kept as an event.
DELETE FROM payments p USING payments q
 WHERE p.order_id = q.order_id AND (p.created_at < q.created_at OR (p.created_at = q.created_at AND p.id < q.id));

ALTER TABLE payments DROP COLUMN idempotency_key;
ALTER TABLE payments ADD COLUMN user_id UUID;
ALTER TABLE payments ADD COLUMN status_detail VARCHAR(100);
ALTER TABLE payments ADD COLUMN checkout_url VARCHAR(1000);
ALTER TABLE payments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD CONSTRAINT uq_payments_order UNIQUE (order_id);
ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (status IN
    ('PENDING', 'IN_PROCESS', 'APPROVED', 'REJECTED', 'CANCELLED', 'REFUNDED'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_amount CHECK (amount > 0);

CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    provider_payment_id VARCHAR(64) NOT NULL,
    provider_status VARCHAR(40) NOT NULL,
    provider_status_detail VARCHAR(100),
    amount DECIMAL(12, 2),
    currency VARCHAR(3),
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Idempotency: the same provider payment in the same state is processed once.
    CONSTRAINT uq_payment_events_provider UNIQUE (provider_payment_id, provider_status)
);

CREATE INDEX idx_payment_events_payment ON payment_events(payment_id, created_at);
CREATE INDEX idx_payments_status ON payments(status);
