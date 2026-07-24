-- Shema baze riječi (Cloudflare D1).
-- Primjena:  npx wrangler d1 execute spy-words --remote --file=./schema.sql

CREATE TABLE IF NOT EXISTS words (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  text       TEXT    NOT NULL,
  -- Mala slova; služi samo za sprječavanje duplikata neovisno o veličini slova.
  normalized TEXT    NOT NULL UNIQUE,
  created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE INDEX IF NOT EXISTS words_created_at ON words (created_at);
