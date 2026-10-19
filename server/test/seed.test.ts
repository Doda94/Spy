import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { body, call, runSeed } from "./helpers";

async function counts() {
  const rows = await env.DB.prepare(
    `SELECT d.id, d.revision,
            (SELECT COUNT(*) FROM categories c WHERE c.dictionary_id = d.id) AS categories,
            (SELECT COUNT(*) FROM words w WHERE w.dictionary_id = d.id) AS words
       FROM dictionaries d ORDER BY d.position`
  ).all<{ id: string; revision: number; categories: number; words: number }>();
  return rows.results;
}

describe("seed.sql", () => {
  it("fills Hrvatski from data/words.json and English from data/words.en.json", async () => {
    expect(await counts()).toEqual([
      expect.objectContaining({ id: "hr", categories: 3, words: 50 }),
      expect.objectContaining({ id: "en", categories: 3, words: 50 }),
    ]);
  });

  it("is safe to run again: no duplicates, revision goes up", async () => {
    const before = await counts();
    await runSeed();
    const after = await counts();

    expect(after.map(({ id, categories, words }) => ({ id, categories, words }))).toEqual(
      before.map(({ id, categories, words }) => ({ id, categories, words }))
    );
    after.forEach((row, i) => expect(row.revision).toBe(before[i].revision + 1));
  });

  it("brings back a word that was deleted through the API", async () => {
    const detail = await body(await call("/v1/dictionaries/hr"));
    const banka = detail.categories[0].words.find((w: any) => w.text === "Banka");
    await call(`/v1/dictionaries/hr/words/${banka.id}`, { method: "DELETE" });
    expect((await counts())[0].words).toBe(49);

    await runSeed();
    expect((await counts())[0].words).toBe(50);
  });

  it("keeps words added through the API", async () => {
    await call("/v1/dictionaries/hr/words", { method: "POST", json: { text: "Most", category: "Mjesta" } });
    await runSeed();
    expect((await counts())[0].words).toBe(51);
  });
});

describe("schema", () => {
  it("rejects a word whose category belongs to another dictionary", async () => {
    const category = await env.DB.prepare("SELECT id FROM categories WHERE dictionary_id = 'hr' LIMIT 1").first<{ id: number }>();
    const insert = env.DB.prepare(
      "INSERT INTO words (dictionary_id, category_id, text, normalized) VALUES ('en', ?, 'Bridge', 'bridge')"
    ).bind(category!.id);
    await expect(insert.run()).rejects.toThrow();
  });

  it("deletes categories and words together with their dictionary", async () => {
    await env.DB.prepare("DELETE FROM dictionaries WHERE id = 'en'").run();
    const left = await env.DB.prepare("SELECT COUNT(*) AS n FROM words WHERE dictionary_id = 'en'").first<{ n: number }>();
    expect(left!.n).toBe(0);
  });
});
