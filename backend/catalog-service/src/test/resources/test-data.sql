INSERT INTO brands (id, name, description, logo_url, country, created_at) VALUES
(1, 'Rolex', 'Swiss watchmaker', 'https://example.com/rolex.png', 'Switzerland', CURRENT_TIMESTAMP),
(2, 'Omega', 'Precision timing', 'https://example.com/omega.png', 'Switzerland', CURRENT_TIMESTAMP),
(3, 'TAG Heuer', 'Avant-garde', 'https://example.com/tag.png', 'Switzerland', CURRENT_TIMESTAMP);

INSERT INTO categories (id, name, description, slug, created_at) VALUES
(1, 'Sport', 'Sport watches', 'sport', CURRENT_TIMESTAMP),
(2, 'Dress', 'Dress watches', 'dress', CURRENT_TIMESTAMP),
(3, 'Dive', 'Dive watches', 'dive', CURRENT_TIMESTAMP),
(4, 'Chronograph', 'Chronograph watches', 'chronograph', CURRENT_TIMESTAMP);

INSERT INTO products (id, name, description, reference_number, brand_id, category_id, price, currency, stock, case_material, case_diameter, movement_type, water_resistance, crystal_type, dial_color, strap_material, power_reserve, image_url, featured, active, created_at, updated_at) VALUES
('a1b2c3d4-0001-4000-8000-000000000001', 'Submariner Date', 'Legendary diver', '126610LN', 1, 3, 9150.00, 'USD', 5, 'Oystersteel', '41mm', 'AUTOMATIC', '300m', 'Sapphire', 'Black', 'Steel Bracelet', '70h', 'https://example.com/sub.png', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1b2c3d4-0002-4000-8000-000000000002', 'Daytona', 'Racing chrono', '126500LN', 1, 4, 14550.00, 'USD', 3, 'Oystersteel', '40mm', 'AUTOMATIC', '100m', 'Sapphire', 'White', 'Steel Bracelet', '72h', 'https://example.com/day.png', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1b2c3d4-0003-4000-8000-000000000003', 'Speedmaster', 'Moonwatch', '310.30', 2, 4, 6550.00, 'USD', 8, 'Steel', '42mm', 'MANUAL', '50m', 'Hesalite', 'Black', 'Steel Bracelet', '50h', 'https://example.com/speed.png', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1b2c3d4-0004-4000-8000-000000000004', 'Seamaster 300M', 'Diver watch', '210.30', 2, 3, 5550.00, 'USD', 10, 'Steel', '42mm', 'AUTOMATIC', '300m', 'Sapphire', 'Blue', 'Steel Bracelet', '55h', 'https://example.com/sea.png', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1b2c3d4-0005-4000-8000-000000000005', 'Carrera Chronograph', 'Racing heritage', 'CBS2210', 3, 4, 5650.00, 'USD', 9, 'Steel', '42mm', 'AUTOMATIC', '100m', 'Sapphire', 'Blue', 'Steel Bracelet', '80h', 'https://example.com/carrera.png', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('a1b2c3d4-0006-4000-8000-000000000006', 'Inactive Watch', 'Not shown', 'INACTIVE1', 3, 1, 1000.00, 'USD', 0, 'Plastic', '30mm', 'QUARTZ', '30m', 'Mineral', 'White', 'Rubber', 'N/A', 'https://example.com/inactive.png', false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
