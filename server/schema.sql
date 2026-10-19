-- Shema stare, ravne baze riječi (jedna tablica `words`) iz web aplikacije na `main`.
-- Na `v2` je samo preseljena kao polazište; zamjenjuje je shema rječnika
-- (rječnik -> kategorija -> riječ) u sljedećem PR-u. Ne primjenjivati je na
-- produkcijsku bazu `spy-words`.

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
