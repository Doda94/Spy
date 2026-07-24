/**
 * Pravila za riječi, zajednička klijentu i Cloudflare funkcijama, da obje
 * strane jednako čiste unos i jednako prepoznaju duplikate.
 */

export const MAX_WORD_LENGTH = 60;

export type Word = {
  id: number;
  text: string;
};

/** Miče višak razmaka s rubova i iz sredine. */
export function cleanWord(raw: string): string {
  return raw.trim().replace(/\s+/g, " ");
}

/** Ključ za usporedbu duplikata — "Plaža" i "plaža" su ista riječ. */
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
