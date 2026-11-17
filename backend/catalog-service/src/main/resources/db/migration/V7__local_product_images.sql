-- Replaces the remote placeholder images of the original seed with photographs served by the storefront itself.
-- Provenance and licences: docs/IMAGE-SOURCES.md

UPDATE products SET image_url = '/images/products/rolex-submariner-date.webp', gallery_urls = '["/images/products/rolex-submariner-date-detail.webp","/images/products/rolex-submariner-date-bracelet.webp"]' WHERE reference_number = '126610LN';
UPDATE products SET image_url = '/images/products/rolex-daytona.webp', gallery_urls = '["/images/products/rolex-daytona-detail.webp"]' WHERE reference_number = '126500LN';
UPDATE products SET image_url = '/images/products/rolex-datejust-41.webp', gallery_urls = '["/images/products/rolex-datejust-41-detail.webp"]' WHERE reference_number = '126334';
UPDATE products SET image_url = '/images/products/rolex-gmt-master-ii.webp', gallery_urls = '["/images/products/rolex-gmt-master-ii-detail.webp"]' WHERE reference_number = '126710BLRO';
UPDATE products SET image_url = '/images/products/omega-speedmaster-moonwatch.webp', gallery_urls = '["/images/products/omega-speedmaster-moonwatch-detail.webp"]' WHERE reference_number = '310.30.42.50.01.002';
UPDATE products SET image_url = '/images/products/omega-seamaster-300m.webp', gallery_urls = '["/images/products/omega-seamaster-300m-detail.webp"]' WHERE reference_number = '210.30.42.20.03.001';
UPDATE products SET image_url = '/images/products/omega-constellation.webp', gallery_urls = '["/images/products/omega-constellation-detail.webp"]' WHERE reference_number = '131.10.39.20.03.001';
UPDATE products SET image_url = '/images/products/omega-seamaster-planet-ocean.webp', gallery_urls = '["/images/products/omega-seamaster-planet-ocean-detail.webp"]' WHERE reference_number = '215.30.44.21.01.001';
UPDATE products SET image_url = '/images/products/tag-heuer-carrera-chronograph.webp', gallery_urls = '["/images/products/tag-heuer-carrera-chronograph-detail.webp"]' WHERE reference_number = 'CBS2210.BA0882';
UPDATE products SET image_url = '/images/products/tag-heuer-monaco.webp', gallery_urls = '["/images/products/tag-heuer-monaco-detail.webp"]' WHERE reference_number = 'CBL2111.BA0644';
UPDATE products SET image_url = '/images/products/tag-heuer-aquaracer-professional-300.webp', gallery_urls = '["/images/products/tag-heuer-aquaracer-professional-300-detail.webp"]' WHERE reference_number = 'WBP201B.BA0632';
UPDATE products SET image_url = '/images/products/tag-heuer-formula-1.webp', gallery_urls = '["/images/products/tag-heuer-formula-1-detail.webp"]' WHERE reference_number = 'CAZ1011.BA0842';

UPDATE brands SET logo_url = NULL;
UPDATE brands SET image_url = '/images/editorial/collection-sport.webp',
  story = 'Founded in Geneva in 1905, Rolex built its reputation on the waterproof Oyster case and the self-winding Perpetual rotor.' WHERE slug = 'rolex';
UPDATE brands SET image_url = '/images/editorial/collection-chronograph.webp',
  story = 'Omega has timed Olympic Games and accompanied missions to the Moon; its Speedmaster is the best-known chronograph in the world.' WHERE slug = 'omega';
UPDATE brands SET image_url = '/images/products/tag-heuer-monaco.webp',
  story = 'TAG Heuer made its name measuring speed, from pocket stopwatches to the square-cased Monaco chronograph of 1969.' WHERE slug = 'tag-heuer';
