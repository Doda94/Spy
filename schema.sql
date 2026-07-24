-- Shema baze riječi (Cloudflare D1) — za novu, praznu bazu.
-- Postojeću bazu nadograđuje migrations/0001_add_category.sql.
--
-- Primjena:  npx wrangler d1 execute spy-words --remote --file=./schema.sql

CREATE TABLE IF NOT EXISTS words (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  text       TEXT    NOT NULL,
  -- Mala slova; služi samo za sprječavanje duplikata neovisno o veličini slova.
  normalized TEXT    NOT NULL UNIQUE,
  category   TEXT    NOT NULL DEFAULT 'Mjesta',
  created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE INDEX IF NOT EXISTS words_created_at ON words (created_at);
CREATE INDEX IF NOT EXISTS words_category ON words (category);
