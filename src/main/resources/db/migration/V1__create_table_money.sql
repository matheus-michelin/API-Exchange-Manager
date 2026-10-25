CREATE TABLE money (
                       id BIGSERIAL PRIMARY KEY,
                       money_qnt DOUBLE PRECISION NOT NULL DEFAULT 0,
                       money_due DOUBLE PRECISION NOT NULL DEFAULT 0,
                       money_total DOUBLE PRECISION NOT NULL DEFAULT 0
);