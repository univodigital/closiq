-- Seed catalog data aligned with frontend mock datasets (Mumbai MVP)

INSERT INTO category (id, slug, name, description, image_url, featured, sort_order) VALUES
    ('11111111-1111-7111-8111-111111111101', 'wedding', 'Wedding', 'Sangeet, reception, and guest-of-honour edits',
     'https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=600&q=70', TRUE, 1),
    ('11111111-1111-7111-8111-111111111102', 'festival', 'Festival', 'Navratri, Diwali, and celebration wear',
     'https://images.unsplash.com/photo-1533709752211-118fcaf03312?auto=format&fit=crop&w=600&q=70', TRUE, 2),
    ('11111111-1111-7111-8111-111111111103', 'office', 'Office', 'Boardroom-ready and corporate event looks',
     'https://images.unsplash.com/photo-1554568218-0f1715e72254?auto=format&fit=crop&w=600&q=70', TRUE, 3),
    ('11111111-1111-7111-8111-111111111104', 'party', 'Party', 'Cocktail, birthday, and night-out statements',
     'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=600&q=70', TRUE, 4),
    ('11111111-1111-7111-8111-111111111105', 'new-in', 'New in', 'Fresh drops this week',
     'https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=600&q=70', FALSE, 5);

INSERT INTO brand (id, slug, name) VALUES
    ('22222222-2222-7222-8222-222222222201', 'house-of-meera', 'House of Meera'),
    ('22222222-2222-7222-8222-222222222202', 'atelier-noir', 'Atelier Noir'),
    ('22222222-2222-7222-8222-222222222203', 'studio-rhea', 'Studio Rhea');

INSERT INTO promotional_offer (id, title, description, code, discount_type, discount_value, valid_until) VALUES
    ('33333333-3333-7333-8333-333333333301', 'First rental — ₹500 off', 'Use code FIRST500 at checkout',
     'FIRST500', 'FIXED', 500, '2026-09-30T23:59:59Z');

INSERT INTO product (
    id, slug, title, description, category_id, brand_id, price_per_day, deposit_amount, currency_code,
    primary_image_url, city, avg_rating, review_count, includes_trial, featured, trending, status,
    min_rental_days, max_rental_days, published_at, created_at, updated_at
) VALUES
    ('44444444-4444-7444-8444-444444444401', 'emerald-draped-saree', 'Emerald draped saree',
     'Pure silk with zari border. Runs true to size through the blouse.',
     '11111111-1111-7111-8111-111111111101', '22222222-2222-7222-8222-222222222201',
     1299, 3500, 'INR',
     'https://images.unsplash.com/photo-1596783074918-c84cb06531ca?auto=format&fit=crop&w=1200&q=80',
     'Mumbai', 4.9, 47, TRUE, TRUE, FALSE, 'ACTIVE', 1, 14, '2026-07-28T10:00:00Z', now(), now()),
    ('44444444-4444-7444-8444-444444444402', 'ivory-bias-cut-gown', 'Ivory bias-cut gown',
     'Fluid satin gown with cowl neckline. Ideal for cocktail evenings.',
     '11111111-1111-7111-8111-111111111104', '22222222-2222-7222-8222-222222222202',
     2150, 5000, 'INR',
     'https://images.unsplash.com/photo-1568252542512-9fe8fe9c87bb?auto=format&fit=crop&w=1200&q=80',
     'Mumbai', 4.8, 31, TRUE, FALSE, TRUE, 'ACTIVE', 1, 14, '2026-07-25T10:00:00Z', now(), now()),
    ('44444444-4444-7444-8444-444444444403', 'sequin-cocktail-dress', 'Sequin cocktail dress',
     'Hand-embellished sequins with structured shoulders.',
     '11111111-1111-7111-8111-111111111104', '22222222-2222-7222-8222-222222222203',
     950, 2500, 'INR',
     'https://images.unsplash.com/photo-1614251055880-ee96e4803393?auto=format&fit=crop&w=1200&q=80',
     'Mumbai', 4.7, 22, TRUE, FALSE, TRUE, 'ACTIVE', 1, 7, '2026-07-20T10:00:00Z', now(), now());

INSERT INTO product_variant (id, product_id, sku, variant_label, status, sort_order) VALUES
    ('55555555-5555-7555-8555-555555555501', '44444444-4444-7444-8444-444444444401', 'emerald-xs', 'XS', 'ACTIVE', 1),
    ('55555555-5555-7555-8555-555555555502', '44444444-4444-7444-8444-444444444401', 'emerald-s', 'S', 'ACTIVE', 2),
    ('55555555-5555-7555-8555-555555555503', '44444444-4444-7444-8444-444444444401', 'emerald-m', 'M', 'ACTIVE', 3),
    ('55555555-5555-7555-8555-555555555504', '44444444-4444-7444-8444-444444444401', 'emerald-l', 'L', 'INACTIVE', 4),
    ('55555555-5555-7555-8555-555555555505', '44444444-4444-7444-8444-444444444402', 'ivory-xs', 'XS', 'ACTIVE', 1),
    ('55555555-5555-7555-8555-555555555506', '44444444-4444-7444-8444-444444444402', 'ivory-s', 'S', 'ACTIVE', 2),
    ('55555555-5555-7555-8555-555555555507', '44444444-4444-7444-8444-444444444403', 'sequin-s', 'S', 'ACTIVE', 1),
    ('55555555-5555-7555-8555-555555555508', '44444444-4444-7444-8444-444444444403', 'sequin-m', 'M', 'ACTIVE', 2);

INSERT INTO product_image (id, product_id, image_url, alt_text, sort_order) VALUES
    ('66666666-6666-7666-8666-666666666601', '44444444-4444-7444-8444-444444444401',
     'https://images.unsplash.com/photo-1596783074918-c84cb06531ca?auto=format&fit=crop&w=1200&q=80', 'Emerald saree front', 0),
    ('66666666-6666-7666-8666-666666666602', '44444444-4444-7444-8444-444444444401',
     'https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=800&q=80', 'Emerald saree detail', 1);
