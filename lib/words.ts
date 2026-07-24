/**
 * Pravila za riječi i kategorije, zajednička klijentu i Cloudflare funkcijama,
 * da obje strane jednako čiste unos i jednako prepoznaju duplikate.
 */

export const MAX_WORD_LENGTH = 60;
export const MAX_CATEGORY_LENGTH = 40;

/** Kategorije koje dolaze s aplikacijom; korisnik može dodati svoje. */
export const DEFAULT_CATEGORY = "Mjesta";

export type Word = {
  id: number;
  text: string;
  category: string;
};

/** Miče višak razmaka s rubova i iz sredine. */
export function cleanWord(raw: string): string {
  return raw.trim().replace(/\s+/g, " ");
}

/** Ključ za usporedbu — "Plaža" i "plaža" su isto, i za riječi i za kategorije. */
export function wordKey(text: string): string {
  return cleanWord(text).toLocaleLowerCase("hr");
}

export type WordCheck =
  | { ok: true; text: string }
  | { ok: false; reason: "empty" | "too-long" };

export function checkWord(raw: string): WordCheck {
  const text = cleanWord(raw);
  if (!text) return { ok: false, reason: "empty" };
  if (text.length > MAX_WORD_LENGTH) return { ok: false, reason: "too-long" };
  return { ok: true, text };
}

export type CategoryCheck =
  | { ok: true; text: string }
  | { ok: false; reason: "category-empty" | "category-too-long" };

export function checkCategory(raw: string): CategoryCheck {
  const text = cleanWord(raw);
  if (!text) return { ok: false, reason: "category-empty" };
  if (text.length > MAX_CATEGORY_LENGTH) {
    return { ok: false, reason: "category-too-long" };
  }
  return { ok: true, text };
}

/** Popis kategorija koje postoje u bazi, poredan po abecedi. */
export function categoriesOf(words: Word[]): string[] {
  const seen = new Map<string, string>();
  for (const word of words) {
    const key = wordKey(word.category);
    if (!seen.has(key)) seen.set(key, word.category);
  }
  return [...seen.values()].sort((a, b) => a.localeCompare(b, "hr"));
}

/**
 * Ako kategorija već postoji (bez obzira na velika/mala slova), vraća njezin
 * zapisani oblik — tako "mjesta" ne stvara drugu kategoriju uz "Mjesta".
 */
export function canonicalCategory(candidate: string, existing: string[]): string {
  const key = wordKey(candidate);
  return existing.find((c) => wordKey(c) === key) ?? cleanWord(candidate);
}
