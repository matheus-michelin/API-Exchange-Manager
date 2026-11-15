
ALTER TABLE money_add ALTER COLUMN description TYPE VARCHAR(600);

UPDATE money_add SET description = 'Sem descrição' WHERE description = '0';
ALTER TABLE money_add ALTER COLUMN description DROP DEFAULT;


CREATE UNIQUE INDEX users_login_lower_uidx ON users (lower(login));