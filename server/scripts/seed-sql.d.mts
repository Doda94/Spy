// Tipovi za scripts/seed-sql.mjs, da ga testovi mogu uvesti.

export type SeedDictionary = {
  id: string;
  name: string;
  language: string;
  /** Oblik iz data/*.json: kategorija -> riječi. */
  words: Record<string, unknown>;
};

export function buildSeed(dictionaries: SeedDictionary[]): { sql: string; summary: string[] };
