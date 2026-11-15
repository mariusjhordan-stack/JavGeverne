CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    reference_number VARCHAR(100) UNIQUE,
    brand_id BIGINT NOT NULL REFERENCES brands(id),
    category_id BIGINT NOT NULL REFERENCES categories(id),
    price DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    case_material VARCHAR(100),
    case_diameter VARCHAR(50),
    movement_type VARCHAR(20) NOT NULL DEFAULT 'AUTOMATIC',
    water_resistance VARCHAR(50),
    crystal_type VARCHAR(50),
    dial_color VARCHAR(50),
    strap_material VARCHAR(100),
    power_reserve VARCHAR(50),
    image_url VARCHAR(500),
    gallery_urls TEXT,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_featured ON products(featured);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_name ON products(name);
