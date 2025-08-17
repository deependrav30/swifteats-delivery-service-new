-- Flyway V1 migration: initialize orders and outbox schema

CREATE TABLE IF NOT EXISTS orders (
  id BIGSERIAL PRIMARY KEY,
  external_id UUID NOT NULL UNIQUE,
  restaurant_id BIGINT NOT NULL,
  customer_id BIGINT,
  total_cents INT NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS outbox (
  id BIGSERIAL PRIMARY KEY,
  aggregate_type VARCHAR(100) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  type VARCHAR(100) NOT NULL,
  payload JSONB NOT NULL,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_outbox_published ON outbox(published);
CREATE INDEX IF NOT EXISTS idx_orders_external_id ON orders(external_id);
