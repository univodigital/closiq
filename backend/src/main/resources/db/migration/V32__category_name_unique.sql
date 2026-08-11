-- Case-insensitive unique category names (slug remains the canonical unique key).
CREATE UNIQUE INDEX IF NOT EXISTS uk_category_name_lower ON category (LOWER(name));
