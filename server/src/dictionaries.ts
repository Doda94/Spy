import { checkCategory, checkWord, wordKey } from "./words";

export type DictionarySummary = {
  id: string;
  name: string;
  language: string;
  revision: number;
  categoryCount: number;
  wordCount: number;
};

export type WordEntry = { id: number; text: string };
export type CategoryEntry = { id: number; name: string; words: WordEntry[] };
export type DictionaryDetail = {
  id: string;
  name: string;
  language: string;
  revision: number;
  categories: CategoryEntry[];
};

export type AddedWord = { id: number; text: string; category: string };

export type AddResult =
  | { ok: true; word: AddedWord }
  | { ok: false; status: number; error: string; word?: AddedWord };

export type DeleteResult = { ok: true } | { ok: false; status: number; error: string };

const DICTIONARY_ID = /^[a-z0-9]{1,32}$/;

/** Ključevi rječnika su kratki i mala slova ('hr', 'en'); sve ostalo ni ne ide u bazu. */
export function isDictionaryId(id: string): boolean {
  return DICTIONARY_ID.test(id);
}

type DictionaryRow = { id: string; name: string; language: string; revision: number };

export async function listDictionaries(db: D1Database): Promise<DictionarySummary[]> {
  const { results } = await db
    .prepare(
      `SELECT d.id, d.name, d.language, d.revision,
              (SELECT COUNT(*) FROM categories c WHERE c.dictionary_id = d.id) AS categoryCount,
              (SELECT COUNT(*) FROM words w WHERE w.dictionary_id = d.id) AS wordCount
         FROM dictionaries d
        ORDER BY d.position, d.id`
    )
    .all<DictionarySummary>();
  return results;
}

/** Verzija popisa: mijenja se čim se promijeni bilo koji rječnik. Aplikacija je smije samo uspoređivati. */
export function catalogVersion(dictionaries: DictionarySummary[]): string {
  return dictionaries.map((d) => `${d.id}.${d.revision}`).join(",");
}

export async function getDictionary(
  db: D1Database,
  id: string
): Promise<DictionaryDetail | null> {
  if (!isDictionaryId(id)) return null;
  const [dictionary, categories, words] = await db.batch([
    db.prepare("SELECT id, name, language, revision FROM dictionaries WHERE id = ?").bind(id),
    db
      .prepare("SELECT id, name FROM categories WHERE dictionary_id = ? ORDER BY position, id")
      .bind(id),
    db.prepare("SELECT id, category_id, text FROM words WHERE dictionary_id = ?").bind(id),
  ]);

  const row = (dictionary.results as DictionaryRow[])[0];
  if (!row) return null;

  const collator = new Intl.Collator(row.language);
  const byCategory = new Map<number, WordEntry[]>();
  for (const w of words.results as { id: number; category_id: number; text: string }[]) {
    const list = byCategory.get(w.category_id) ?? [];
    list.push({ id: w.id, text: w.text });
    byCategory.set(w.category_id, list);
  }

  return {
    ...row,
    categories: (categories.results as { id: number; name: string }[]).map((c) => ({
      id: c.id,
      name: c.name,
      words: (byCategory.get(c.id) ?? []).sort((a, b) => collator.compare(a.text, b.text)),
    })),
  };
}

export async function addWord(
  db: D1Database,
  dictionaryId: string,
  rawText: string,
  rawCategory: string
): Promise<AddResult> {
  if (!isDictionaryId(dictionaryId)) return { ok: false, status: 404, error: "not_found" };
  const dictionary = await db
    .prepare("SELECT id, language FROM dictionaries WHERE id = ?")
    .bind(dictionaryId)
    .first<{ id: string; language: string }>();
  if (!dictionary) return { ok: false, status: 404, error: "not_found" };

  const word = checkWord(rawText);
  if (!word.ok) return { ok: false, status: 400, error: word.reason };
  const category = checkCategory(rawCategory);
  if (!category.ok) return { ok: false, status: 400, error: category.reason };

  const key = wordKey(word.text, dictionary.language);
  const duplicate = await findWord(db, dictionaryId, key);
  if (duplicate) return { ok: false, status: 409, error: "duplicate", word: duplicate };

  // Nova kategorija koja se samo razlikuje po velikim slovima pripada postojećoj
  // (INSERT OR IGNORE zadržava zapisani oblik), a nova ide na kraj popisa.
  try {
    const results = await db.batch([
      db
        .prepare(
          `INSERT OR IGNORE INTO categories (dictionary_id, name, normalized, position)
           VALUES (?1, ?2, ?3,
                   (SELECT COALESCE(MAX(position), -1) + 1 FROM categories WHERE dictionary_id = ?1))`
        )
        .bind(dictionaryId, category.text, wordKey(category.text, dictionary.language)),
      db
        .prepare(
          `INSERT INTO words (dictionary_id, category_id, text, normalized)
           SELECT ?1, c.id, ?2, ?3 FROM categories c
            WHERE c.dictionary_id = ?1 AND c.normalized = ?4
           RETURNING id, text, (SELECT name FROM categories WHERE id = category_id) AS category`
        )
        .bind(
          dictionaryId,
          word.text,
          key,
          wordKey(category.text, dictionary.language)
        ),
      db.prepare("UPDATE dictionaries SET revision = revision + 1 WHERE id = ?").bind(dictionaryId),
    ]);
    const created = (results[1].results as AddedWord[])[0];
    return { ok: true, word: created };
  } catch (error) {
    // Dvije istodobne izmjene iste riječi: druga je izgubila utrku na UNIQUE ograničenju.
    const raced = await findWord(db, dictionaryId, key);
    if (raced) return { ok: false, status: 409, error: "duplicate", word: raced };
    throw error;
  }
}

export async function deleteWord(
  db: D1Database,
  dictionaryId: string,
  wordId: number
): Promise<DeleteResult> {
  if (!isDictionaryId(dictionaryId)) return { ok: false, status: 404, error: "not_found" };
  const existing = await db
    .prepare("SELECT id FROM words WHERE id = ? AND dictionary_id = ?")
    .bind(wordId, dictionaryId)
    .first();
  if (!existing) return { ok: false, status: 404, error: "not_found" };

  await db.batch([
    db.prepare("DELETE FROM words WHERE id = ? AND dictionary_id = ?").bind(wordId, dictionaryId),
    // Kategorije se izvode iz riječi: nestane kad se obriše zadnja riječ.
    db
      .prepare(
        `DELETE FROM categories
          WHERE dictionary_id = ? AND NOT EXISTS (SELECT 1 FROM words WHERE category_id = categories.id)`
      )
      .bind(dictionaryId),
    db.prepare("UPDATE dictionaries SET revision = revision + 1 WHERE id = ?").bind(dictionaryId),
  ]);
  return { ok: true };
}

function findWord(db: D1Database, dictionaryId: string, key: string): Promise<AddedWord | null> {
  return db
    .prepare(
      `SELECT w.id, w.text, c.name AS category
         FROM words w JOIN categories c ON c.id = w.category_id
        WHERE w.dictionary_id = ? AND w.normalized = ?`
    )
    .bind(dictionaryId, key)
    .first<AddedWord>();
}
