import { env, exports } from "cloudflare:workers";
import seedSql from "../seed.sql?raw";

export const PIN = "1234";

/** Pojedinačne naredbe iz SQL-a seeda (bez komentara), spremne za `db.batch`. */
export function seedStatements(sql: string = seedSql): string[] {
  return sql
    .split("\n")
    .filter((line) => !line.startsWith("--"))
    .join("\n")
    .split(/;\s*\n/)
    .map((statement) => statement.trim())
    .filter(Boolean);
}

/** Pokreće seed.sql, ili drugi SQL iz `buildSeed` kad test treba izmijenjene podatke. */
export async function runSeed(sql: string = seedSql): Promise<void> {
  await env.DB.batch(seedStatements(sql).map((statement) => env.DB.prepare(statement)));
}

/** Prazna baza pa seed — svaki test kreće od istog stanja. */
export async function resetDatabase(): Promise<void> {
  await env.DB.batch([
    env.DB.prepare("DELETE FROM words"),
    env.DB.prepare("DELETE FROM categories"),
    env.DB.prepare("DELETE FROM dictionaries"),
    env.DB.prepare("DELETE FROM pin_failures"),
    env.DB.prepare("DELETE FROM sqlite_sequence WHERE name IN ('words', 'categories')"),
  ]);
  await runSeed();
}

type Init = RequestInit & { pin?: string | null; json?: unknown };

/** Zahtjev prema Workeru; `pin` postavlja `x-spy-pin` (zadano ispravan PIN za izmjene). */
export function call(path: string, init: Init = {}): Promise<Response> {
  const { pin, json, ...rest } = init;
  const headers = new Headers(rest.headers);
  const method = (rest.method ?? "GET").toUpperCase();
  const pinToSend = pin === undefined ? (method === "GET" || method === "HEAD" ? null : PIN) : pin;
  if (pinToSend !== null) headers.set("x-spy-pin", pinToSend);
  if (json !== undefined) {
    headers.set("content-type", "application/json");
    rest.body = JSON.stringify(json);
  }
  return exports.default.fetch(new Request(`https://spy.test${path}`, { ...rest, headers }));
}

export async function body<T = any>(response: Response): Promise<T> {
  return (await response.json()) as T;
}
