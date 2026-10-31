// Pretvara data/words.json (hrvatski) i data/words.en.json (engleski) u server/seed.sql.
// Pokretanje (iz mape server/):  npm run seed:generate

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
// Ista pravila čišćenja i usporedbe kao na serveru (Node pokreće .ts bez prevođenja).
import { cleanWord, wordKey } from "../src/words.ts";

// server/scripts/ -> server/ (mapa u koju se piše seed.sql) -> korijen repozitorija (data/).
const serverDir = join(dirname(fileURLToPath(import.meta.url)), "..");
const dataDir = join(serverDir, "..", "data");

// `id` je ključ koji aplikacije pamte pa ga ne treba mijenjati; `language` određuje
// kako se riječi uspoređuju (velika/mala slova) i kako se sortiraju.
const DICTIONARIES = [
  { id: "hr", name: "Hrvatski", language: "hr", file: "words.json" },
  { id: "en", name: "English", language: "en", file: "words.en.json" },
];

const quote = (value) => `'${value.replace(/'/g, "''")}'`;

const statements = [];
const summary = [];

DICTIONARIES.forEach((dictionary, position) => {
  const { id, name, language } = dictionary;
  const byCategory = JSON.parse(readFileSync(join(dataDir, dictionary.file), "utf8"));

  statements.push(
    `INSERT INTO dictionaries (id, name, language, position, revision)\n` +
      `VALUES (${quote(id)}, ${quote(name)}, ${quote(language)}, ${position}, unixepoch())\n` +
      `ON CONFLICT (id) DO UPDATE SET name = excluded.name, language = excluded.language, position = excluded.position;`
  );

  const seen = new Set();
  let categoryCount = 0;
  let wordCount = 0;

  Object.entries(byCategory).forEach(([rawCategory, words]) => {
    const category = cleanWord(String(rawCategory));
    if (!category || !Array.isArray(words)) return;
    const categoryKey = wordKey(category, language);

    const rows = [];
    for (const raw of words) {
      const text = cleanWord(String(raw));
      if (!text) continue;
      const key = wordKey(text, language);
      // Riječ je jedinstvena u cijelom rječniku, ne samo unutar kategorije.
      if (seen.has(key)) continue;
      seen.add(key);
      rows.push(`    (${quote(text)}, ${quote(key)})`);
    }

    statements.push(
      `INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)\n` +
        `VALUES (${quote(id)}, ${quote(category)}, ${quote(categoryKey)}, ${categoryCount});`
    );
    categoryCount++;

    if (rows.length > 0) {
      statements.push(
        `WITH v (text, normalized) AS (VALUES\n${rows.join(",\n")}\n)\n` +
          `INSERT OR IGNORE INTO words (dictionary_id, category_id, text, normalized)\n` +
          `SELECT ${quote(id)}, c.id, v.text, v.normalized FROM v, categories c\n` +
          ` WHERE c.dictionary_id = ${quote(id)} AND c.normalized = ${quote(categoryKey)};`
      );
      wordCount += rows.length;
    }
  });

  summary.push(`${id}: ${wordCount} riječi u ${categoryCount} kategorija`);
});

// Pomiče verziju svih rječnika pa aplikacije ponovno dohvate sadržaj.
statements.push("UPDATE dictionaries SET revision = revision + 1;");

const sql = `-- Generirano iz data/words.json i data/words.en.json (npm run seed:generate u mapi server/).
-- Ne uređivati ručno; uredi JSON pa ponovno pokreni skriptu.
--
-- Sigurno za ponovno pokretanje: rječnici se ažuriraju, a kategorije i riječi dodaju s
-- "INSERT OR IGNORE", pa nema duplikata. Riječi koje fale u bazi (npr. obrisane kroz API)
-- ponovno se dodaju: JSON je izvor istine za početni sadržaj.
-- Svako pokretanje podiže verziju rječnika, pa aplikacije ponovno dohvate sadržaj.
--
-- Primjena (iz mape server/):  npm run db:setup:local   ili   npm run db:setup:remote

${statements.join("\n\n")}
`;

writeFileSync(join(serverDir, "seed.sql"), sql);
console.log(`seed.sql: ${summary.join("; ")}`);
