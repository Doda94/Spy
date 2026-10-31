// Pretvara data/words.json (hrvatski) i data/words.en.json (engleski) u server/seed.sql.
// Pokretanje (iz mape server/):  npm run seed:generate

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { buildSeed } from "./seed-sql.mjs";

// server/scripts/ -> server/ (mapa u koju se piše seed.sql) -> korijen repozitorija (data/).
const serverDir = join(dirname(fileURLToPath(import.meta.url)), "..");
const dataDir = join(serverDir, "..", "data");

// `id` je ključ koji aplikacije pamte pa ga ne treba mijenjati; `language` određuje
// kako se riječi uspoređuju (velika/mala slova) i kako se sortiraju. Jezik postojećeg
// rječnika ne može se promijeniti (vidi okidač u migracijama).
const DICTIONARIES = [
  { id: "hr", name: "Hrvatski", language: "hr", file: "words.json" },
  { id: "en", name: "English", language: "en", file: "words.en.json" },
];

const dictionaries = DICTIONARIES.map(({ file, ...dictionary }) => ({
  ...dictionary,
  words: JSON.parse(readFileSync(join(dataDir, file), "utf8")),
}));
const { sql, summary } = buildSeed(dictionaries);

writeFileSync(join(serverDir, "seed.sql"), sql);
console.log(`seed.sql: ${summary.join("; ")}`);
