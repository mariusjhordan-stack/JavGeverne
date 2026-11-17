-- Stock is reserved per order so that releasing or confirming it is idempotent and auditable.
CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12, 2) NOT NULL CHECK (unit_price >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_reservation_order_product UNIQUE (order_id, product_id)
);

CREATE INDEX idx_stock_reservations_order ON stock_reservations(order_id);

CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    delta INTEGER NOT NULL CHECK (delta <> 0),
    stock_after INTEGER NOT NULL CHECK (stock_after >= 0),
    reason VARCHAR(30) NOT NULL,
    reference VARCHAR(255),
    actor VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_product ON stock_movements(product_id, created_at DESC);
CREATE INDEX idx_stock_movements_created ON stock_movements(created_at DESC);

ALTER TABLE products ADD CONSTRAINT chk_products_price_non_negative CHECK (price >= 0);

ALTER TABLE brands ADD COLUMN slug VARCHAR(100);
ALTER TABLE brands ADD COLUMN story TEXT;
ALTER TABLE brands ADD COLUMN image_url VARCHAR(500);
UPDATE brands SET slug = lower(replace(name, ' ', '-'));
ALTER TABLE brands ALTER COLUMN slug SET NOT NULL;
ALTER TABLE brands ADD CONSTRAINT uq_brands_slug UNIQUE (slug);
