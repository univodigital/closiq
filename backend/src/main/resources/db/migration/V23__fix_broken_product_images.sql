-- Replace removed Unsplash photo IDs used by men/kids seed products.
UPDATE product
SET primary_image_url = 'https://images.unsplash.com/photo-1614251055880-ee96e4803393?auto=format&fit=crop&w=1200&q=80',
    updated_at = now()
WHERE slug = 'midnight-embroidered-sherwani';

UPDATE product
SET primary_image_url = 'https://images.unsplash.com/photo-1554568218-0f1715e72254?auto=format&fit=crop&w=1200&q=80',
    updated_at = now()
WHERE slug = 'blush-kids-lehenga-set';

UPDATE product_image
SET image_url = 'https://images.unsplash.com/photo-1614251055880-ee96e4803393?auto=format&fit=crop&w=1200&q=80'
WHERE product_id = '44444444-4444-7444-8444-444444444404'
  AND image_url LIKE '%photo-1593030761757%';

UPDATE product_image
SET image_url = 'https://images.unsplash.com/photo-1554568218-0f1715e72254?auto=format&fit=crop&w=1200&q=80'
WHERE product_id = '44444444-4444-7444-8444-444444444405'
  AND image_url LIKE '%photo-1519238263530%';
