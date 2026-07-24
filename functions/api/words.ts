import { checkWord, wordKey, type Word } from "../../lib/words";

type Env = {
  DB: D1Database;
  ADMIN_PIN?: string;
};

const noStore = { "cache-control": "no-store" };

/** GET /api/words — cijeli popis, otvoren svima. */
export const onRequestGet: PagesFunction<Env> = async ({ env }) => {
  const { results } = await env.DB.prepare(
    "SELECT id, text FROM words ORDER BY normalized"
  ).all<Word>();

  return Response.json({ words: results ?? [] }, { headers: noStore });
};

/** POST /api/words — dodavanje riječi (PIN provjerava _middleware). */
export const onRequestPost: PagesFunction<Env> = async ({ request, env }) => {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return Response.json({ error: "invalid_body" }, { status: 400, headers: noStore });
  }

  const raw = (body as { text?: unknown })?.text;
  if (typeof raw !== "string") {
    return Response.json({ error: "invalid_body" }, { status: 400, headers: noStore });
  }

  const check = checkWord(raw);
  if (!check.ok) {
    return Response.json({ error: check.reason }, { status: 400, headers: noStore });
  }

  const existing = await env.DB.prepare("SELECT id, text FROM words WHERE normalized = ?")
    .bind(wordKey(check.text))
    .first<Word>();
  if (existing) {
    return Response.json(
      { error: "duplicate", word: existing },
      { status: 409, headers: noStore }
    );
  }

  const created = await env.DB.prepare(
    "INSERT INTO words (text, normalized) VALUES (?, ?) RETURNING id, text"
  )
    .bind(check.text, wordKey(check.text))
    .first<Word>();

  return Response.json({ word: created }, { status: 201, headers: noStore });
};
