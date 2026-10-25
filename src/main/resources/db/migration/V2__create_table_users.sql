CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       login VARCHAR(100) NOT NULL UNIQUE,
                       senha VARCHAR(255) NOT NULL
);

DELETE FROM money;

ALTER TABLE money DROP COLUMN money_qnt, DROP COLUMN money_due;
ALTER TABLE money ADD COLUMN user_id BIGINT NOT NULL UNIQUE REFERENCES users(id);