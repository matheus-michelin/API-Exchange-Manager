ALTER TABLE money_add ADD COLUMN balance_after DOUBLE PRECISION;
UPDATE money_add SET balance_after = amount WHERE balance_after IS NULL;
ALTER TABLE money_add ALTER COLUMN balance_after SET NOT NULL;