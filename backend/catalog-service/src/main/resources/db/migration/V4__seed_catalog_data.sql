-- Brands
INSERT INTO brands (id, name, description, logo_url, country) VALUES
(1, 'Rolex', 'Crown jewel of Swiss watchmaking since 1905. Synonymous with prestige, precision, and enduring value.', 'https://placehold.co/200x80/1a1a2e/gold?text=Rolex', 'Switzerland'),
(2, 'Omega', 'Pioneer of precision timing since 1848. From the Moon to the ocean depths, Omega defines adventure.', 'https://placehold.co/200x80/1a1a2e/gold?text=Omega', 'Switzerland'),
(3, 'TAG Heuer', 'Bold Swiss avant-garde since 1860. The fusion of luxury and cutting-edge performance.', 'https://placehold.co/200x80/1a1a2e/gold?text=TAG+Heuer', 'Switzerland');

SELECT setval('brands_id_seq', 3);

-- Categories
INSERT INTO categories (id, name, description, slug) VALUES
(1, 'Sport', 'High-performance timepieces built for the active lifestyle', 'sport'),
(2, 'Dress', 'Elegant watches crafted for formal occasions and refined taste', 'dress'),
(3, 'Dive', 'Professional diving instruments engineered for extreme depths', 'dive'),
(4, 'Chronograph', 'Precision timing complications for the discerning collector', 'chronograph');

SELECT setval('categories_id_seq', 4);

-- Products: Rolex
INSERT INTO products (id, name, description, reference_number, brand_id, category_id, price, currency, stock, case_material, case_diameter, movement_type, water_resistance, crystal_type, dial_color, strap_material, power_reserve, image_url, gallery_urls, featured, active) VALUES
('a1b2c3d4-0001-4000-8000-000000000001', 'Rolex Submariner Date', 'The reference among divers'' watches. Launched in 1953, the Submariner was the first watch waterproof to a depth of 100 metres.', '126610LN', 1, 3, 9150.00, 'USD', 5, 'Oystersteel', '41mm', 'AUTOMATIC', '300m', 'Sapphire Crystal', 'Black', 'Oystersteel Bracelet', '70 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Submariner', '[]', true, true),

('a1b2c3d4-0002-4000-8000-000000000002', 'Rolex Daytona', 'Born for speed. The Cosmograph Daytona is the ultimate timing instrument for endurance racing drivers.', '126500LN', 1, 4, 14550.00, 'USD', 3, 'Oystersteel', '40mm', 'AUTOMATIC', '100m', 'Sapphire Crystal', 'White', 'Oystersteel Bracelet', '72 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Daytona', '[]', true, true),

('a1b2c3d4-0003-4000-8000-000000000003', 'Rolex Datejust 41', 'The archetype of the classic watch. A timeless symbol of style and performance since 1945.', '126334', 1, 2, 10250.00, 'USD', 7, 'Oystersteel & White Gold', '41mm', 'AUTOMATIC', '100m', 'Sapphire Crystal', 'Blue', 'Jubilee Bracelet', '70 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Datejust', '[]', false, true),

('a1b2c3d4-0004-4000-8000-000000000004', 'Rolex GMT-Master II', 'The cosmopolitan watch. Designed to show the time in two different time zones simultaneously.', '126710BLRO', 1, 1, 10800.00, 'USD', 4, 'Oystersteel', '40mm', 'AUTOMATIC', '100m', 'Sapphire Crystal', 'Black', 'Jubilee Bracelet', '70 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=GMT-Master', '[]', true, true),

-- Products: Omega
('a1b2c3d4-0005-4000-8000-000000000005', 'Omega Speedmaster Moonwatch', 'The first watch worn on the Moon. Flight-qualified by NASA for all manned space missions since 1965.', '310.30.42.50.01.002', 2, 4, 6550.00, 'USD', 8, 'Stainless Steel', '42mm', 'MANUAL', '50m', 'Hesalite Crystal', 'Black', 'Stainless Steel Bracelet', '50 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Speedmaster', '[]', true, true),

('a1b2c3d4-0006-4000-8000-000000000006', 'Omega Seamaster 300M', 'The legendary diver chosen by Commander Bond. Master Chronometer certified for ultimate precision.', '210.30.42.20.03.001', 2, 3, 5550.00, 'USD', 10, 'Stainless Steel', '42mm', 'AUTOMATIC', '300m', 'Sapphire Crystal', 'Blue', 'Stainless Steel Bracelet', '55 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Seamaster', '[]', true, true),

('a1b2c3d4-0007-4000-8000-000000000007', 'Omega Constellation', 'The epitome of elegant precision. A distinctive design that has symbolized accuracy since 1952.', '131.10.39.20.03.001', 2, 2, 5250.00, 'USD', 6, 'Stainless Steel', '39mm', 'AUTOMATIC', '50m', 'Sapphire Crystal', 'Blue', 'Stainless Steel Bracelet', '55 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Constellation', '[]', false, true),

('a1b2c3d4-0008-4000-8000-000000000008', 'Omega Seamaster Planet Ocean', 'Professional diving instrument inspired by the ocean. Built to perform at depths of 600 metres.', '215.30.44.21.01.001', 2, 3, 6850.00, 'USD', 4, 'Stainless Steel', '43.5mm', 'AUTOMATIC', '600m', 'Sapphire Crystal', 'Black', 'Stainless Steel Bracelet', '55 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Planet+Ocean', '[]', false, true),

-- Products: TAG Heuer
('a1b2c3d4-0009-4000-8000-000000000009', 'TAG Heuer Carrera Chronograph', 'Born on the racetrack. The Carrera has been the definitive motorsport chronograph since 1963.', 'CBS2210.BA0882', 3, 4, 5650.00, 'USD', 9, 'Stainless Steel', '42mm', 'AUTOMATIC', '100m', 'Sapphire Crystal', 'Blue', 'Stainless Steel Bracelet', '80 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Carrera', '[]', true, true),

('a1b2c3d4-0010-4000-8000-000000000010', 'TAG Heuer Monaco', 'The icon of avant-garde watchmaking. The world''s first automatic square-cased chronograph.', 'CBL2111.BA0644', 3, 4, 6550.00, 'USD', 3, 'Stainless Steel', '39mm', 'AUTOMATIC', '100m', 'Sapphire Crystal', 'Blue', 'Stainless Steel Bracelet', '80 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Monaco', '[]', false, true),

('a1b2c3d4-0011-4000-8000-000000000011', 'TAG Heuer Aquaracer Professional 300', 'Engineered for extreme aquatic performance. A bold diving watch built to conquer the depths.', 'WBP201B.BA0632', 3, 3, 2950.00, 'USD', 12, 'Stainless Steel', '43mm', 'AUTOMATIC', '300m', 'Sapphire Crystal', 'Black', 'Stainless Steel Bracelet', '38 hours', 'https://placehold.co/600x600/1a1a2e/gold?text=Aquaracer', '[]', false, true),

('a1b2c3d4-0012-4000-8000-000000000012', 'TAG Heuer Formula 1', 'The spirit of Formula 1 on your wrist. Sporty, precise, and built for high performance.', 'CAZ1011.BA0842', 3, 1, 1650.00, 'USD', 15, 'Stainless Steel', '43mm', 'QUARTZ', '200m', 'Sapphire Crystal', 'Black', 'Stainless Steel Bracelet', 'N/A', 'https://placehold.co/600x600/1a1a2e/gold?text=Formula+1', '[]', false, true);
