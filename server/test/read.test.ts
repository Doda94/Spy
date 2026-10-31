import { describe, expect, it } from "vitest";
import { body, call } from "./helpers";

describe("GET /v1/dictionaries", () => {
  it("lists Hrvatski and English with counts and a version", async () => {
    const response = await call("/v1/dictionaries");
    expect(response.status).toBe(200);
    expect(response.headers.get("cache-control")).toBe("no-cache");

    const { version, dictionaries } = await body(response);
    expect(dictionaries).toEqual([
      expect.objectContaining({ id: "hr", name: "Hrvatski", language: "hr", categoryCount: 3, wordCount: 50 }),
      expect.objectContaining({ id: "en", name: "English", language: "en", categoryCount: 3, wordCount: 50 }),
    ]);
    expect(version).toBe(dictionaries.map((d: any) => `${d.id}.${d.revision}`).join(","));
    expect(response.headers.get("etag")).toBe(`W/"${version}"`);
  });

  it("answers 304 when the client already has the version", async () => {
    const first = await call("/v1/dictionaries");
    const etag = first.headers.get("etag")!;

    const second = await call("/v1/dictionaries", { headers: { "if-none-match": etag } });
    expect(second.status).toBe(304);
    expect(second.headers.get("etag")).toBe(etag);
    expect(await second.text()).toBe("");

    const strong = etag.replace("W/", "");
    expect((await call("/v1/dictionaries", { headers: { "if-none-match": `"x", ${strong}` } })).status).toBe(304);
    expect((await call("/v1/dictionaries", { headers: { "if-none-match": "*" } })).status).toBe(304);
    expect((await call("/v1/dictionaries", { headers: { "if-none-match": 'W/"stale"' } })).status).toBe(200);
  });

  it("needs no PIN", async () => {
    const response = await call("/v1/dictionaries", { pin: null });
    expect(response.status).toBe(200);
  });
});

describe("GET /v1/dictionaries/:id", () => {
  it("returns dictionary -> category -> words in seed order", async () => {
    const response = await call("/v1/dictionaries/hr");
    expect(response.status).toBe(200);

    const detail = await body(response);
    expect(detail).toMatchObject({ id: "hr", name: "Hrvatski", language: "hr" });
    expect(detail.categories.map((c: any) => c.name)).toEqual(["Mjesta", "Zanimanja", "Predmeti"]);
    expect(detail.categories.map((c: any) => c.words.length)).toEqual([22, 14, 14]);
    expect(detail.categories[0].words[0]).toEqual({ id: expect.any(Number), text: "Banka" });
  });

  it("sorts words with the dictionary's own language", async () => {
    const hr = await body(await call("/v1/dictionaries/hr"));
    const objects = hr.categories[2].words.map((w: any) => w.text);
    // Uz hrvatski poredak "Š" dolazi iza "S" (a ne iza "Z", kao u binarnom poretku).
    const order = ["Ruksak", "Svijeća", "Šalica", "Škare"];
    expect(objects.filter((text: string) => order.includes(text))).toEqual(order);

    const en = await body(await call("/v1/dictionaries/en"));
    expect(en).toMatchObject({ name: "English", language: "en" });
    expect(en.categories.map((c: any) => c.name)).toEqual(["Places", "Jobs", "Objects"]);
    expect(en.categories[0].words.map((w: any) => w.text).slice(0, 3)).toEqual(["Airplane", "Army base", "Bank"]);
  });

  it("supports HEAD and conditional requests", async () => {
    const head = await call("/v1/dictionaries/hr", { method: "HEAD" });
    expect(head.status).toBe(200);
    const etag = head.headers.get("etag")!;
    expect(etag).toMatch(/^W\/"hr\.\d+"$/);

    const cached = await call("/v1/dictionaries/hr", { headers: { "if-none-match": etag } });
    expect(cached.status).toBe(304);
  });

  it("404 for unknown and malformed ids", async () => {
    for (const id of ["xx", "HR", "hr%20", "a".repeat(40)]) {
      const response = await call(`/v1/dictionaries/${id}`);
      expect(response.status).toBe(404);
      expect(await body(response)).toEqual({ error: "not_found" });
    }
  });
});

describe("routing", () => {
  it("serves a service banner at /", async () => {
    expect(await body(await call("/"))).toEqual({ service: "spy-v2", api: "v1" });
  });

  it("404 for unknown paths, 405 with Allow for wrong methods", async () => {
    expect((await call("/nope")).status).toBe(404);
    expect((await call("/api/words")).status).toBe(404);

    const wrong = await call("/v1/dictionaries", { method: "DELETE" });
    expect(wrong.status).toBe(405);
    expect(wrong.headers.get("allow")).toBe("GET, HEAD");
    expect((await call("/v1/dictionaries/hr/words", { method: "GET" })).status).toBe(405);
    expect((await call("/v1/dictionaries/hr/words/1", { method: "POST" })).status).toBe(405);
  });

  it("tolerates a trailing slash", async () => {
    expect((await call("/v1/dictionaries/")).status).toBe(200);
  });
});
