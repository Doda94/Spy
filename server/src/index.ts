import { requirePin } from "./auth";
import {
  addWord,
  catalogVersion,
  deleteWord,
  getDictionary,
  listDictionaries,
} from "./dictionaries";
import { cacheable, fail, json, weakEtag, type Env } from "./http";

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      return await route(request, env);
    } catch (error) {
      console.error(error);
      return fail("server", 500);
    }
  },
} satisfies ExportedHandler<Env>;

async function route(request: Request, env: Env): Promise<Response> {
  const { pathname } = new URL(request.url);
  const path = pathname.length > 1 ? pathname.replace(/\/+$/, "") : pathname;
  const method = request.method;
  const isRead = method === "GET" || method === "HEAD";

  if (path === "/") {
    if (!isRead) return fail("method_not_allowed", 405, {}, { allow: "GET, HEAD" });
    return json({ service: "spy-v2", api: "v1" });
  }

  if (path === "/v1/dictionaries") {
    if (!isRead) return fail("method_not_allowed", 405, {}, { allow: "GET, HEAD" });
    const dictionaries = await listDictionaries(env.DB);
    const version = catalogVersion(dictionaries);
    return cacheable(request, { version, dictionaries }, weakEtag(version));
  }

  if (path === "/v1/auth") {
    if (method !== "POST") return fail("method_not_allowed", 405, {}, { allow: "POST" });
    return (await requirePin(request, env)) ?? json({ ok: true });
  }

  const dictionary = path.match(/^\/v1\/dictionaries\/([^/]+)$/);
  if (dictionary) {
    if (!isRead) return fail("method_not_allowed", 405, {}, { allow: "GET, HEAD" });
    const detail = await getDictionary(env.DB, dictionary[1]);
    if (!detail) return fail("not_found", 404);
    return cacheable(request, detail, weakEtag(`${detail.id}.${detail.revision}`));
  }

  const words = path.match(/^\/v1\/dictionaries\/([^/]+)\/words$/);
  if (words) {
    if (method !== "POST") return fail("method_not_allowed", 405, {}, { allow: "POST" });
    const denied = await requirePin(request, env);
    if (denied) return denied;

    let body: unknown;
    try {
      body = await request.json();
    } catch {
      return fail("invalid_body", 400);
    }
    const { text, category } = (body ?? {}) as { text?: unknown; category?: unknown };
    if (typeof text !== "string" || typeof category !== "string") {
      return fail("invalid_body", 400);
    }

    const result = await addWord(env.DB, words[1], text, category);
    if (!result.ok) {
      return fail(result.error, result.status, result.word ? { word: result.word } : {});
    }
    return json({ word: result.word }, 201);
  }

  const word = path.match(/^\/v1\/dictionaries\/([^/]+)\/words\/([^/]+)$/);
  if (word) {
    if (method !== "DELETE") return fail("method_not_allowed", 405, {}, { allow: "DELETE" });
    const denied = await requirePin(request, env);
    if (denied) return denied;

    // Samo zapis cijelog broja: Number() bi prihvatio i "1e3", "0x10" ili "12.0".
    if (!/^[1-9]\d{0,15}$/.test(word[2])) return fail("invalid_id", 400);
    const id = Number(word[2]);

    const result = await deleteWord(env.DB, word[1], id);
    if (!result.ok) return fail(result.error, result.status);
    return new Response(null, { status: 204, headers: { "cache-control": "no-store" } });
  }

  return fail("not_found", 404);
}
