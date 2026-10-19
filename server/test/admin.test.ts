import { env } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import worker from "../src/index";
import { body, call } from "./helpers";

async function revision(id: string): Promise<number> {
  const list = await body(await call("/v1/dictionaries"));
  return list.dictionaries.find((d: any) => d.id === id).revision;
}

describe("PIN", () => {
  it("POST /v1/auth accepts the right PIN", async () => {
    const response = await call("/v1/auth", { method: "POST" });
    expect(response.status).toBe(200);
    expect(await body(response)).toEqual({ ok: true });
  });

  it("rejects a missing or wrong PIN on every write", async () => {
    for (const pin of [null, "", "0000", "12345", "123"]) {
      const attempts = [
        call("/v1/auth", { method: "POST", pin }),
        call("/v1/dictionaries/hr/words", { method: "POST", pin, json: { text: "Test", category: "Mjesta" } }),
        call("/v1/dictionaries/hr/words/1", { method: "DELETE", pin }),
      ];
      for (const response of await Promise.all(attempts)) {
        expect(response.status).toBe(401);
        expect(await body(response)).toEqual({ error: "unauthorized" });
      }
    }
    const list = await body(await call("/v1/dictionaries"));
    expect(list.dictionaries[0].wordCount).toBe(50);
  });

  it("answers 503 when ADMIN_PIN is not configured", async () => {
    const request = new Request("https://spy.test/v1/auth", {
      method: "POST",
      headers: { "x-spy-pin": "1234" },
    });
    const response = await worker.fetch(request, { ...env, ADMIN_PIN: undefined });
    expect(response.status).toBe(503);
    expect(await body(response)).toEqual({ error: "pin_not_configured" });
  });

  it("never caches write responses", async () => {
    const denied = await call("/v1/auth", { method: "POST", pin: "nope" });
    expect(denied.headers.get("cache-control")).toBe("no-store");
  });
});

describe("POST /v1/dictionaries/:id/words", () => {
  it("adds a word to an existing category and bumps the revision", async () => {
    const before = await revision("hr");
    const englishBefore = await revision("en");
    const response = await call("/v1/dictionaries/hr/words", {
      method: "POST",
      json: { text: "  Most  ", category: "Mjesta" },
    });
    expect(response.status).toBe(201);
    expect(await body(response)).toEqual({ word: { id: expect.any(Number), text: "Most", category: "Mjesta" } });

    expect(await revision("hr")).toBe(before + 1);
    expect(await revision("en")).toBe(englishBefore);
    const detail = await body(await call("/v1/dictionaries/hr"));
    expect(detail.categories[0].words.map((w: any) => w.text)).toContain("Most");
    expect(detail.categories).toHaveLength(3);
  });

  it("changes the catalogue and dictionary ETags, only for the touched dictionary", async () => {
    const list = await call("/v1/dictionaries");
    const en = await call("/v1/dictionaries/en");
    await call("/v1/dictionaries/hr/words", { method: "POST", json: { text: "Most", category: "Mjesta" } });

    expect((await call("/v1/dictionaries", { headers: { "if-none-match": list.headers.get("etag")! } })).status).toBe(200);
    expect((await call("/v1/dictionaries/en", { headers: { "if-none-match": en.headers.get("etag")! } })).status).toBe(304);
  });

  it("reuses a category regardless of case and keeps its stored spelling", async () => {
    const response = await call("/v1/dictionaries/hr/words", {
      method: "POST",
      json: { text: "Most", category: "  mjesta " },
    });
    expect(response.status).toBe(201);
    expect((await body(response)).word.category).toBe("Mjesta");
    expect((await body(await call("/v1/dictionaries/hr"))).categories).toHaveLength(3);
  });

  it("creates a new category at the end", async () => {
    const response = await call("/v1/dictionaries/en/words", {
      method: "POST",
      json: { text: "Pizza", category: "Food" },
    });
    expect(response.status).toBe(201);

    const detail = await body(await call("/v1/dictionaries/en"));
    expect(detail.categories.map((c: any) => c.name)).toEqual(["Places", "Jobs", "Objects", "Food"]);
    expect(detail.categories[3].words.map((w: any) => w.text)).toEqual(["Pizza"]);
  });

  it("rejects duplicates case-insensitively across categories, per dictionary", async () => {
    const duplicate = await call("/v1/dictionaries/hr/words", {
      method: "POST",
      json: { text: "ŠKOLA", category: "Zanimanja" },
    });
    expect(duplicate.status).toBe(409);
    expect(await body(duplicate)).toEqual({
      error: "duplicate",
      word: { id: expect.any(Number), text: "Škola", category: "Mjesta" },
    });

    // "Hotel" postoji u hrvatskom, ali engleski rječnik ima svoj skup riječi.
    const other = await call("/v1/dictionaries/en/words", {
      method: "POST",
      json: { text: "Hotel", category: "Places" },
    });
    expect(other.status).toBe(409);
    const unique = await call("/v1/dictionaries/en/words", {
      method: "POST",
      json: { text: "Vlak", category: "Places" },
    });
    expect(unique.status).toBe(201);
  });

  it("validates the body", async () => {
    const add = (json: unknown) => call("/v1/dictionaries/hr/words", { method: "POST", json });
    const code = async (response: Response) => (await body(response)).error;

    expect(await code(await add({ text: "   ", category: "Mjesta" }))).toBe("empty");
    expect(await code(await add({ text: "x".repeat(61), category: "Mjesta" }))).toBe("too-long");
    expect(await code(await add({ text: "Most", category: " " }))).toBe("category-empty");
    expect(await code(await add({ text: "Most", category: "x".repeat(41) }))).toBe("category-too-long");
    expect(await code(await add({ text: 5, category: "Mjesta" }))).toBe("invalid_body");
    expect(await code(await add(null))).toBe("invalid_body");

    const notJson = await call("/v1/dictionaries/hr/words", { method: "POST", body: "{nope" });
    expect(notJson.status).toBe(400);
    expect(await code(notJson)).toBe("invalid_body");

    const exact = await add({ text: "x".repeat(60), category: "y".repeat(40) });
    expect(exact.status).toBe(201);
  });

  it("404 for an unknown dictionary (after the PIN check)", async () => {
    const response = await call("/v1/dictionaries/de/words", {
      method: "POST",
      json: { text: "Haus", category: "Orte" },
    });
    expect(response.status).toBe(404);
    const unauthorized = await call("/v1/dictionaries/de/words", {
      method: "POST",
      pin: "x",
      json: { text: "Haus", category: "Orte" },
    });
    expect(unauthorized.status).toBe(401);
  });
});

describe("DELETE /v1/dictionaries/:id/words/:wordId", () => {
  async function findWord(dictionary: string, text: string) {
    const detail = await body(await call(`/v1/dictionaries/${dictionary}`));
    for (const category of detail.categories) {
      const word = category.words.find((w: any) => w.text === text);
      if (word) return word.id as number;
    }
    throw new Error(`no ${text}`);
  }

  it("deletes a word, bumps the revision and answers 204", async () => {
    const id = await findWord("hr", "Banka");
    const before = await revision("hr");

    const response = await call(`/v1/dictionaries/hr/words/${id}`, { method: "DELETE" });
    expect(response.status).toBe(204);
    expect(response.headers.get("cache-control")).toBe("no-store");

    expect(await revision("hr")).toBe(before + 1);
    const detail = await body(await call("/v1/dictionaries/hr"));
    expect(detail.categories[0].words.map((w: any) => w.text)).not.toContain("Banka");
  });

  it("drops a category when its last word is deleted", async () => {
    const added = await body(
      await call("/v1/dictionaries/en/words", { method: "POST", json: { text: "Pizza", category: "Food" } })
    );
    expect((await body(await call("/v1/dictionaries/en"))).categories).toHaveLength(4);

    await call(`/v1/dictionaries/en/words/${added.word.id}`, { method: "DELETE" });
    const detail = await body(await call("/v1/dictionaries/en"));
    expect(detail.categories.map((c: any) => c.name)).toEqual(["Places", "Jobs", "Objects"]);
  });

  it("404 for a missing word or a word of another dictionary", async () => {
    const id = await findWord("hr", "Banka");
    expect((await call("/v1/dictionaries/hr/words/999999", { method: "DELETE" })).status).toBe(404);
    expect((await call(`/v1/dictionaries/en/words/${id}`, { method: "DELETE" })).status).toBe(404);
    expect((await call("/v1/dictionaries/zz/words/1", { method: "DELETE" })).status).toBe(404);
    // Riječ u pravom rječniku je ostala.
    expect(await findWord("hr", "Banka")).toBe(id);
  });

  it("400 for a bad id", async () => {
    for (const id of ["abc", "0", "-3", "1.5"]) {
      const response = await call(`/v1/dictionaries/hr/words/${id}`, { method: "DELETE" });
      expect(response.status).toBe(400);
      expect(await body(response)).toEqual({ error: "invalid_id" });
    }
  });
});
