-- Generirano iz data/words.json i data/words.en.json (npm run seed:generate u mapi server/).
-- Ne uređivati ručno; uredi JSON pa ponovno pokreni skriptu.
--
-- Sigurno za ponovno pokretanje: rječnici se ažuriraju, a kategorije i riječi dodaju s
-- "INSERT OR IGNORE", pa nema duplikata. Riječi koje fale u bazi (npr. obrisane kroz API)
-- ponovno se dodaju: JSON je izvor istine za početni sadržaj.
-- Svako pokretanje podiže verziju rječnika, pa aplikacije ponovno dohvate sadržaj.
--
-- Primjena (iz mape server/):  npm run db:setup:local   ili   npm run db:setup:remote

INSERT INTO dictionaries (id, name, language, position, revision)
VALUES ('hr', 'Hrvatski', 'hr', 0, unixepoch())
ON CONFLICT (id) DO UPDATE SET name = excluded.name, language = excluded.language, position = excluded.position;

INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)
VALUES ('hr', 'Mjesta', 'mjesta', 0);

WITH v (text, normalized) AS (VALUES
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
    ('Svjetionik', 'svjetionik')
)
INSERT OR IGNORE INTO words (dictionary_id, category_id, text, normalized)
SELECT 'hr', c.id, v.text, v.normalized FROM v, categories c
 WHERE c.dictionary_id = 'hr' AND c.normalized = 'mjesta';

INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)
VALUES ('hr', 'Zanimanja', 'zanimanja', 1);

WITH v (text, normalized) AS (VALUES
    ('Liječnik', 'liječnik'),
    ('Učiteljica', 'učiteljica'),
    ('Vatrogasac', 'vatrogasac'),
    ('Policajac', 'policajac'),
    ('Kuhar', 'kuhar'),
    ('Pilot', 'pilot'),
    ('Glumac', 'glumac'),
    ('Vozač autobusa', 'vozač autobusa'),
    ('Frizer', 'frizer'),
    ('Novinar', 'novinar'),
    ('Zubar', 'zubar'),
    ('Građevinar', 'građevinar'),
    ('Ribar', 'ribar'),
    ('Sudac', 'sudac')
)
INSERT OR IGNORE INTO words (dictionary_id, category_id, text, normalized)
SELECT 'hr', c.id, v.text, v.normalized FROM v, categories c
 WHERE c.dictionary_id = 'hr' AND c.normalized = 'zanimanja';

INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)
VALUES ('hr', 'Predmeti', 'predmeti', 2);

WITH v (text, normalized) AS (VALUES
    ('Kišobran', 'kišobran'),
    ('Naočale', 'naočale'),
    ('Ključevi', 'ključevi'),
    ('Ručni sat', 'ručni sat'),
    ('Ruksak', 'ruksak'),
    ('Svijeća', 'svijeća'),
    ('Ogledalo', 'ogledalo'),
    ('Škare', 'škare'),
    ('Jastuk', 'jastuk'),
    ('Bicikl', 'bicikl'),
    ('Gitara', 'gitara'),
    ('Fotoaparat', 'fotoaparat'),
    ('Četkica za zube', 'četkica za zube'),
    ('Šalica', 'šalica')
)
INSERT OR IGNORE INTO words (dictionary_id, category_id, text, normalized)
SELECT 'hr', c.id, v.text, v.normalized FROM v, categories c
 WHERE c.dictionary_id = 'hr' AND c.normalized = 'predmeti';

INSERT INTO dictionaries (id, name, language, position, revision)
VALUES ('en', 'English', 'en', 1, unixepoch())
ON CONFLICT (id) DO UPDATE SET name = excluded.name, language = excluded.language, position = excluded.position;

INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)
VALUES ('en', 'Places', 'places', 0);

WITH v (text, normalized) AS (VALUES
    ('Beach', 'beach'),
    ('Hospital', 'hospital'),
    ('Airplane', 'airplane'),
    ('School', 'school'),
    ('Theater', 'theater'),
    ('Submarine', 'submarine'),
    ('Space station', 'space station'),
    ('Football stadium', 'football stadium'),
    ('Restaurant', 'restaurant'),
    ('Train', 'train'),
    ('Bank', 'bank'),
    ('Circus', 'circus'),
    ('Zoo', 'zoo'),
    ('Police station', 'police station'),
    ('Ski resort', 'ski resort'),
    ('Library', 'library'),
    ('Fire station', 'fire station'),
    ('Cinema', 'cinema'),
    ('Market', 'market'),
    ('Hotel', 'hotel'),
    ('Army base', 'army base'),
    ('Lighthouse', 'lighthouse')
)
INSERT OR IGNORE INTO words (dictionary_id, category_id, text, normalized)
SELECT 'en', c.id, v.text, v.normalized FROM v, categories c
 WHERE c.dictionary_id = 'en' AND c.normalized = 'places';

INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)
VALUES ('en', 'Jobs', 'jobs', 1);

WITH v (text, normalized) AS (VALUES
    ('Doctor', 'doctor'),
    ('Teacher', 'teacher'),
    ('Firefighter', 'firefighter'),
    ('Police officer', 'police officer'),
    ('Cook', 'cook'),
    ('Pilot', 'pilot'),
    ('Actor', 'actor'),
    ('Bus driver', 'bus driver'),
    ('Hairdresser', 'hairdresser'),
    ('Journalist', 'journalist'),
    ('Dentist', 'dentist'),
    ('Builder', 'builder'),
    ('Fisherman', 'fisherman'),
    ('Judge', 'judge')
)
INSERT OR IGNORE INTO words (dictionary_id, category_id, text, normalized)
SELECT 'en', c.id, v.text, v.normalized FROM v, categories c
 WHERE c.dictionary_id = 'en' AND c.normalized = 'jobs';

INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)
VALUES ('en', 'Objects', 'objects', 2);

WITH v (text, normalized) AS (VALUES
    ('Umbrella', 'umbrella'),
    ('Glasses', 'glasses'),
    ('Keys', 'keys'),
    ('Wristwatch', 'wristwatch'),
    ('Backpack', 'backpack'),
    ('Candle', 'candle'),
    ('Mirror', 'mirror'),
    ('Scissors', 'scissors'),
    ('Pillow', 'pillow'),
    ('Bicycle', 'bicycle'),
    ('Guitar', 'guitar'),
    ('Camera', 'camera'),
    ('Toothbrush', 'toothbrush'),
    ('Mug', 'mug')
)
INSERT OR IGNORE INTO words (dictionary_id, category_id, text, normalized)
SELECT 'en', c.id, v.text, v.normalized FROM v, categories c
 WHERE c.dictionary_id = 'en' AND c.normalized = 'objects';

UPDATE dictionaries SET revision = revision + 1;
