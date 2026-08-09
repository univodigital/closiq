ALTER TABLE product
    ADD COLUMN audience VARCHAR(10),
    ADD COLUMN garment_type VARCHAR(50);

UPDATE product SET audience = 'women', garment_type = 'sarees' WHERE slug = 'emerald-draped-saree';
UPDATE product SET audience = 'women', garment_type = 'gowns' WHERE slug = 'ivory-bias-cut-gown';
UPDATE product SET audience = 'women', garment_type = 'indo-western' WHERE slug = 'sequin-cocktail-dress';

ALTER TABLE product
    ALTER COLUMN audience SET NOT NULL;

INSERT INTO product (
    id, product_code, slug, title, description, category_id, brand_id, price_per_day, deposit_amount, currency_code,
    primary_image_url, city, avg_rating, review_count, includes_trial, featured, trending, status,
    min_rental_days, max_rental_days, audience, garment_type, published_at, created_at, updated_at
) VALUES
    ('44444444-4444-7444-8444-444444444404', 'VST-PROD-100004', 'midnight-embroidered-sherwani', 'Midnight embroidered sherwani',
     'Silk sherwani with tonal embroidery. Ideal for wedding ceremonies and sangeet nights.',
     '11111111-1111-7111-8111-111111111101', '22222222-2222-7222-8222-222222222202',
     1899, 4500, 'INR',
     'https://images.unsplash.com/photo-1593030761757-71cae45d48e7?auto=format&fit=crop&w=1200&q=80',
     'Mumbai', 4.8, 19, TRUE, TRUE, FALSE, 'ACTIVE', 1, 14, 'men', 'sherwanis',
     '2026-07-22T10:00:00Z', now(), now()),
    ('44444444-4444-7444-8444-444444444405', 'VST-PROD-100005', 'blush-kids-lehenga-set', 'Blush kids lehenga set',
     'Lightweight lehenga with soft dupatta. Comfortable for festivals and family weddings.',
     '11111111-1111-7111-8111-111111111102', '22222222-2222-7222-8222-222222222201',
     650, 1500, 'INR',
     'https://images.unsplash.com/photo-1519238263530-822dce4332bd?auto=format&fit=crop&w=1200&q=80',
     'Mumbai', 4.9, 12, TRUE, FALSE, TRUE, 'ACTIVE', 1, 7, 'kids', 'lehengas',
     '2026-07-18T10:00:00Z', now(), now());

INSERT INTO product_variant (id, product_id, sku, variant_label, status, sort_order) VALUES
    ('55555555-5555-7555-8555-555555555509', '44444444-4444-7444-8444-444444444404', 'sherwani-m', 'M', 'ACTIVE', 1),
    ('55555555-5555-7555-8555-555555555510', '44444444-4444-7444-8444-444444444404', 'sherwani-l', 'L', 'ACTIVE', 2),
    ('55555555-5555-7555-8555-555555555511', '44444444-4444-7444-8444-444444444405', 'kids-lehenga-6', '6Y', 'ACTIVE', 1),
    ('55555555-5555-7555-8555-555555555512', '44444444-4444-7444-8444-444444444405', 'kids-lehenga-8', '8Y', 'ACTIVE', 2);

INSERT INTO product_image (id, product_id, image_url, alt_text, sort_order) VALUES
    ('66666666-6666-7666-8666-666666666603', '44444444-4444-7444-8444-444444444404',
     'https://images.unsplash.com/photo-1593030761757-71cae45d48e7?auto=format&fit=crop&w=1200&q=80', 'Sherwani front', 0),
    ('66666666-6666-7666-8666-666666666604', '44444444-4444-7444-8444-444444444405',
     'https://images.unsplash.com/photo-1519238263530-822dce4332bd?auto=format&fit=crop&w=1200&q=80', 'Kids lehenga', 0);
