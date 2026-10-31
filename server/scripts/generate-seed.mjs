// Pretvara data/words.json u server/seed.sql.
// Pokretanje (iz mape server/):  npm run seed:generate

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
// Ista pravila čišćenja i usporedbe kao na serveru (Node pokreće .ts bez prevođenja).
import { cleanWord, wordKey } from "../src/words.ts";

// server/scripts/ -> server/ (mapa u koju se piše seed.sql) -> korijen repozitorija (data/words.json).
const serverDir = join(dirname(fileURLToPath(import.meta.url)), "..");
const byCategory = JSON.parse(
  readFileSync(join(serverDir, "..", "data", "words.json"), "utf8")
);

const quote = (value) => `'${value.replace(/'/g, "''")}'`;

const rows = [];
const seen = new Set();
for (const [rawCategory, words] of Object.entries(byCategory)) {
  const category = cleanWord(String(rawCategory));
  if (!category || !Array.isArray(words)) continue;

  for (const raw of words) {
    const text = cleanWord(String(raw));
    if (!text) continue;
    const normalized = wordKey(text);
    // Riječ je jedinstvena u cijeloj bazi, ne samo unutar kategorije.
    if (seen.has(normalized)) continue;
    seen.add(normalized);
    rows.push(`  (${quote(text)}, ${quote(normalized)}, ${quote(category)})`);
  }
}

const sql = `-- Generirano iz data/words.json (npm run seed:generate u mapi server/).
-- Ne uređivati ručno; uredi words.json pa ponovno pokreni skriptu.
--
-- "OR IGNORE" znači da ponovno pokretanje ne stvara duplikate. Riječi koje si u
-- međuvremenu obrisao kroz aplikaciju ponovno se dodaju, jer njihovog retka više nema.

INSERT OR IGNORE INTO words (text, normalized, category) VALUES
${rows.join(",\n")};
`;

writeFileSync(join(serverDir, "seed.sql"), sql);
console.log(
  `seed.sql: ${rows.length} riječi u ${Object.keys(byCategory).length} kategorija`
);
