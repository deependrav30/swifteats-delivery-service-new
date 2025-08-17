-- Flyway V1 migration: initialize catalog schema (restaurants + menu items)

CREATE TABLE IF NOT EXISTS restaurants (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  address TEXT,
  is_open BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS menu_items (
  id BIGSERIAL PRIMARY KEY,
  restaurant_id BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  price_cents INT NOT NULL,
  available BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes to optimize common lookups
CREATE INDEX IF NOT EXISTS idx_menu_items_restaurant_id ON menu_items(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_restaurants_is_open ON restaurants(is_open);

-- Sample seed data (optional for local development)
INSERT INTO restaurants (name, address, is_open)
SELECT 'Demo Diner', '123 Demo St, Pune, MH', true
WHERE NOT EXISTS (SELECT 1 FROM restaurants WHERE name = 'Demo Diner');

INSERT INTO menu_items (restaurant_id, name, description, price_cents, available)
SELECT r.id, 'Masala Dosa', 'Crispy dosa with potato masala', 12000, true
FROM restaurants r
WHERE r.name = 'Demo Diner'
  AND NOT EXISTS (
    SELECT 1 FROM menu_items m WHERE m.restaurant_id = r.id AND m.name = 'Masala Dosa'
  );
