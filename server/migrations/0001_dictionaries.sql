-- Ugrađeni rječnici: rječnik -> kategorija -> riječ, s jezikom po rječniku.
-- Primjena (iz mape server/):  npx wrangler d1 migrations apply spy-dictionaries --remote

CREATE TABLE dictionaries (
  -- Stabilan kratki ključ koji aplikacije pamte (npr. 'hr', 'en').
  id         TEXT    PRIMARY KEY CHECK (id GLOB '[a-z0-9]*' AND length(id) <= 32),
  name       TEXT    NOT NULL,
  -- Jezik riječi (ISO 639-1, npr. 'hr'); određuje i pravila velikih/malih slova.
  language   TEXT    NOT NULL CHECK (length(language) BETWEEN 2 AND 3),
  position   INTEGER NOT NULL DEFAULT 0,
  -- Raste sa svakom izmjenom sadržaja; aplikacije ga samo uspoređuju na jednakost (ETag).
  revision   INTEGER NOT NULL DEFAULT 1,
  created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE TABLE categories (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  dictionary_id TEXT    NOT NULL REFERENCES dictionaries (id) ON DELETE CASCADE,
  name          TEXT    NOT NULL,
  -- Mala slova; "Mjesta" i "mjesta" su ista kategorija unutar rječnika.
  normalized    TEXT    NOT NULL,
  position      INTEGER NOT NULL DEFAULT 0,
  UNIQUE (dictionary_id, normalized),
  UNIQUE (id, dictionary_id)
);

CREATE TABLE words (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  dictionary_id TEXT    NOT NULL,
  category_id   INTEGER NOT NULL,
  text          TEXT    NOT NULL,
  -- Riječ je jedinstvena u cijelom rječniku, ne samo unutar kategorije.
  normalized    TEXT    NOT NULL,
  created_at    INTEGER NOT NULL DEFAULT (unixepoch()),
  UNIQUE (dictionary_id, normalized),
  -- Kategorija mora pripadati istom rječniku kao riječ.
  FOREIGN KEY (category_id, dictionary_id) REFERENCES categories (id, dictionary_id) ON DELETE CASCADE
);

CREATE INDEX words_category ON words (category_id);
