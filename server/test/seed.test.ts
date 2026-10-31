import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import hr from "../../data/words.json";
import en from "../../data/words.en.json";
import seedSql from "../seed.sql?raw";
import { buildSeed } from "../scripts/seed-sql.mjs";
import { body, call, runSeed } from "./helpers";

type Words = Record<string, string[]>;

/** SQL seeda kao iz generate-seed.mjs, ali s izmijenjenim podacima. */
function seedWith(change: { hr?: Words; en?: Words; hrLanguage?: string } = {}): string {
  return buildSeed([
    { id: "hr", name: "Hrvatski", language: change.hrLanguage ?? "hr", words: change.hr ?? hr },
    { id: "en", name: "English", language: "en", words: change.en ?? en },
  ]).sql;
}

async function categoryNames(id: string): Promise<string[]> {
  return (await body(await call(`/v1/dictionaries/${id}`))).categories.map((c: any) => c.name);
}

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

  it("is safe to run again: no duplicates, and no new revision when nothing changed", async () => {
    const before = await counts();
    await runSeed();
    expect(await counts()).toEqual(before);
  });

  it("matches what generate-seed.mjs wrote", () => {
    expect(seedWith()).toBe(seedSql);
  });

  it("bumps only the dictionary whose content changed", async () => {
    const detail = await body(await call("/v1/dictionaries/hr"));
    const banka = detail.categories[0].words.find((w: any) => w.text === "Banka");
    await call(`/v1/dictionaries/hr/words/${banka.id}`, { method: "DELETE" });
    const before = await counts();

    await runSeed();
    const after = await counts();
    expect(after[0].revision).toBeGreaterThan(before[0].revision);
    expect(after[1].revision).toBe(before[1].revision);
  });

  it("skips a category with no words", async () => {
    await runSeed(seedWith({ en: { ...en, Food: [], Sports: ["  "] } }));
    expect(await categoryNames("en")).toEqual(["Places", "Jobs", "Objects"]);
  });

  it("moves a word whose category changed in the JSON", async () => {
    const { Mjesta, Zanimanja, ...rest } = hr as Words;
    await runSeed(
      seedWith({ hr: { Mjesta: Mjesta.filter((w) => w !== "Banka"), Zanimanja: [...Zanimanja, "Banka"], ...rest } })
    );
    const detail = await body(await call("/v1/dictionaries/hr"));
    const where = detail.categories.filter((c: any) => c.words.some((w: any) => w.text === "Banka"));
    expect(where.map((c: any) => c.name)).toEqual(["Zanimanja"]);
    expect((await counts())[0].words).toBe(50);
  });

  it("drops a category whose words all moved elsewhere", async () => {
    const { Places, Jobs, Objects } = en as Words;
    await runSeed(seedWith({ en: { Places: [...Places, ...Jobs], Objects } }));
    expect(await categoryNames("en")).toEqual(["Places", "Objects"]);
    expect((await counts())[1].words).toBe(50);
  });

  it("puts a new JSON category after one added through the API", async () => {
    await call("/v1/dictionaries/hr/words", { method: "POST", json: { text: "Pizza", category: "Hrana" } });
    await runSeed(seedWith({ hr: { ...(hr as Words), Sport: ["Tenis"] } }));
    expect(await categoryNames("hr")).toEqual(["Mjesta", "Zanimanja", "Predmeti", "Hrana", "Sport"]);
  });

  it("refuses to change the language of an existing dictionary", async () => {
    await expect(runSeed(seedWith({ hrLanguage: "en" }))).rejects.toThrow(/language/);
    expect((await counts())[0]).toEqual(expect.objectContaining({ id: "hr", words: 50 }));
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
