// Gradi seed.sql iz sadržaja rječnika. Bez pristupa datotekama, da ga mogu zvati i testovi
// (test/seed.test.ts) s izmijenjenim podacima; datoteke čita scripts/generate-seed.mjs.

import { cleanWord, wordKey } from "../src/words.ts";

const quote = (value) => `'${value.replace(/'/g, "''")}'`;

/**
 * @param {{ id: string, name: string, language: string, words: Record<string, unknown> }[]} dictionaries
 *   rječnici redom prikaza; `words` je oblik iz data/*.json (kategorija -> riječi)
 * @returns {{ sql: string, summary: string[] }}
 */
export function buildSeed(dictionaries) {
  const statements = [];
  const summary = [];

  dictionaries.forEach((dictionary, position) => {
    const { id, name, language } = dictionary;
    const byCategory = dictionary.words;
    const dict = quote(id);
    // Verzija raste samo ako je prethodna naredba nešto promijenila, pa ponovni seed bez
    // promjena ne tjera aplikacije da sve ponovno preuzmu.
    const bump = `UPDATE dictionaries SET revision = revision + 1 WHERE id = ${dict} AND changes() > 0;`;

    statements.push(
      `INSERT INTO dictionaries (id, name, language, position, revision)\n` +
        `VALUES (${dict}, ${quote(name)}, ${quote(language)}, ${position}, unixepoch())\n` +
        `ON CONFLICT (id) DO UPDATE SET name = excluded.name, language = excluded.language, position = excluded.position\n` +
        ` WHERE name <> excluded.name OR language <> excluded.language OR position <> excluded.position;`,
      bump
    );

    const seen = new Set();
    let categoryCount = 0;
    let wordCount = 0;

    for (const [rawCategory, words] of Object.entries(byCategory)) {
      const category = cleanWord(String(rawCategory));
      if (!category || !Array.isArray(words)) continue;
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
      // Kategorije se izvode iz riječi: ona bez ijedne riječi ne ide u bazu.
      if (rows.length === 0) continue;

      // Nova kategorija ide na kraj, kao i ona dodana kroz API, pa se redoslijedi ne sudaraju.
      statements.push(
        `INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)\n` +
          `SELECT ${dict}, ${quote(category)}, ${quote(categoryKey)}, COALESCE(MAX(position), -1) + 1\n` +
          `  FROM categories WHERE dictionary_id = ${dict};`,
        // Riječ koja je u JSON-u premještena u drugu kategoriju premješta se i u bazi.
        `WITH v (text, normalized) AS (VALUES\n${rows.join(",\n")}\n)\n` +
          `INSERT INTO words (dictionary_id, category_id, text, normalized)\n` +
          `SELECT ${dict}, c.id, v.text, v.normalized FROM v, categories c\n` +
          ` WHERE c.dictionary_id = ${dict} AND c.normalized = ${quote(categoryKey)}\n` +
          `ON CONFLICT (dictionary_id, normalized) DO UPDATE SET category_id = excluded.category_id, text = excluded.text\n` +
          ` WHERE words.category_id <> excluded.category_id OR words.text <> excluded.text;`,
        bump
      );
      categoryCount++;
      wordCount += rows.length;
    }

    // Kategorija iz koje su sve riječi premještene nestaje, kao i kod brisanja kroz API.
    statements.push(
      `DELETE FROM categories\n` +
        ` WHERE dictionary_id = ${dict} AND NOT EXISTS (SELECT 1 FROM words WHERE category_id = categories.id);`,
      bump
    );

    summary.push(`${id}: ${wordCount} riječi u ${categoryCount} kategorija`);
  });

  const sql = `-- Generirano iz data/words.json i data/words.en.json (npm run seed:generate u mapi server/).
-- Ne uređivati ručno; uredi JSON pa ponovno pokreni skriptu.
--
-- Sigurno za ponovno pokretanje: JSON je izvor istine za početni sadržaj.
-- - Riječi koje fale u bazi (npr. obrisane kroz API) ponovno se dodaju.
-- - Riječ premještena u drugu kategoriju u JSON-u premješta se i u bazi.
-- - Riječi dodane kroz API ostaju.
-- - Verzija rječnika raste samo ako se njegov sadržaj promijenio.
--
-- Primjena (iz mape server/):  npm run db:setup:local   ili   npm run db:setup:remote

${statements.join("\n\n")}
`;
  return { sql, summary };
}
