UPDATE orders SET status = 'PENDING' WHERE status = 'CONFIRMED';

ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN expires_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN cancel_reason VARCHAR(100);
ALTER TABLE orders ADD COLUMN stock_release_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (status IN
    ('PENDING', 'PAYMENT_PENDING', 'PAYMENT_FAILED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'));
ALTER TABLE orders ADD CONSTRAINT chk_orders_total CHECK (total >= 0 AND subtotal >= 0 AND tax >= 0);

CREATE INDEX idx_orders_expiry ON orders(status, expires_at);

CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_status_history_order ON order_status_history(order_id, created_at);
