-- Generirano iz data/words.json (node scripts/generate-seed.mjs).
-- Ne uređivati ručno; uredi words.json pa ponovno pokreni skriptu.
-- Primjena:  npx wrangler d1 execute spy-words --remote --file=./seed.sql
--
-- "OR IGNORE" znači da ponovno pokretanje ne stvara duplikate i ne dira
-- riječi koje si u međuvremenu dodao ili obrisao kroz aplikaciju.

INSERT OR IGNORE INTO words (text, normalized) VALUES
  ('Plaža', 'plaža'),
  ('Bolnica', 'bolnica'),
  ('Zrakoplov', 'zrakoplov'),
  ('Škola', 'škola'),
  ('Kazalište', 'kazalište'),
  ('Podmornica', 'podmornica'),
  ('Svemirska postaja', 'svemirska postaja'),
  ('Nogometni stadion', 'nogometni stadion'),
  ('Restoran', 'restoran'),
  ('Vlak', 'vlak'),
  ('Banka', 'banka'),
  ('Cirkus', 'cirkus'),
  ('Zoološki vrt', 'zoološki vrt'),
  ('Policijska postaja', 'policijska postaja'),
  ('Skijalište', 'skijalište'),
  ('Knjižnica', 'knjižnica'),
  ('Vatrogasni dom', 'vatrogasni dom'),
  ('Kino', 'kino'),
  ('Tržnica', 'tržnica'),
  ('Hotel', 'hotel'),
  ('Vojarna', 'vojarna'),
  ('Svjetionik', 'svjetionik');
