// Pretvara data/words.json u seed.sql.
// Pokretanje:  node scripts/generate-seed.mjs

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const byCategory = JSON.parse(readFileSync(join(root, "data/words.json"), "utf8"));

const quote = (value) => `'${value.replace(/'/g, "''")}'`;
const clean = (value) => String(value).trim().replace(/\s+/g, " ");

const rows = [];
const seen = new Set();
for (const [rawCategory, words] of Object.entries(byCategory)) {
  const category = clean(rawCategory);
  if (!category || !Array.isArray(words)) continue;

  for (const raw of words) {
    const text = clean(raw);
    if (!text) continue;
    const normalized = text.toLocaleLowerCase("hr");
    // Riječ je jedinstvena u cijeloj bazi, ne samo unutar kategorije.
    if (seen.has(normalized)) continue;
    seen.add(normalized);
    rows.push(`  (${quote(text)}, ${quote(normalized)}, ${quote(category)})`);
  }
}

const sql = `-- Generirano iz data/words.json (node scripts/generate-seed.mjs).
-- Ne uređivati ručno; uredi words.json pa ponovno pokreni skriptu.
-- Primjena:  npx wrangler d1 execute spy-words --remote --file=./seed.sql
--
-- "OR IGNORE" znači da ponovno pokretanje ne stvara duplikate i ne dira
-- riječi koje si u međuvremenu dodao ili obrisao kroz aplikaciju.

INSERT OR IGNORE INTO words (text, normalized, category) VALUES
${rows.join(",\n")};
`;

writeFileSync(join(root, "seed.sql"), sql);
console.log(
  `seed.sql: ${rows.length} riječi u ${Object.keys(byCategory).length} kategorija`
);
