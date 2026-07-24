import {
  canonicalCategory,
  checkCategory,
  checkWord,
  wordKey,
  type Word,
} from "../../lib/words";

type Env = {
  DB: D1Database;
  ADMIN_PIN?: string;
};

const noStore = { "cache-control": "no-store" };

/** GET /api/words — cijeli popis, otvoren svima. */
export const onRequestGet: PagesFunction<Env> = async ({ env }) => {
  const { results } = await env.DB.prepare(
    "SELECT id, text, category FROM words ORDER BY category, normalized"
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

  const { text: rawText, category: rawCategory } = (body ?? {}) as {
    text?: unknown;
    category?: unknown;
  };
  if (typeof rawText !== "string" || typeof rawCategory !== "string") {
    return Response.json({ error: "invalid_body" }, { status: 400, headers: noStore });
  }

  const word = checkWord(rawText);
  if (!word.ok) {
    return Response.json({ error: word.reason }, { status: 400, headers: noStore });
  }

  const category = checkCategory(rawCategory);
  if (!category.ok) {
    return Response.json({ error: category.reason }, { status: 400, headers: noStore });
  }

  const existing = await env.DB.prepare(
    "SELECT id, text, category FROM words WHERE normalized = ?"
  )
    .bind(wordKey(word.text))
    .first<Word>();
  if (existing) {
    return Response.json(
      { error: "duplicate", word: existing },
      { status: 409, headers: noStore }
    );
  }

  // Nova kategorija koja se samo razlikuje po velikim slovima pripada postojećoj.
  const { results: known } = await env.DB.prepare(
    "SELECT DISTINCT category FROM words"
  ).all<{ category: string }>();
  const finalCategory = canonicalCategory(
    category.text,
    (known ?? []).map((row) => row.category)
  );

  const created = await env.DB.prepare(
    "INSERT INTO words (text, normalized, category) VALUES (?, ?, ?) RETURNING id, text, category"
  )
    .bind(word.text, wordKey(word.text), finalCategory)
    .first<Word>();

  return Response.json({ word: created }, { status: 201, headers: noStore });
};
