CREATE TABLE money_add (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL REFERENCES users(id),
                           amount DOUBLE PRECISION NOT NULL,
                           received_on DATE NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE money ADD COLUMN IF NOT EXISTS money_qnt DOUBLE PRECISION NOT NULL DEFAULT 0;