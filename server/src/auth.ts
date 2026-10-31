import { fail, type Env } from "./http";

/** Koliko krivih PIN-ova smije doći s jedne adrese u [CLIENT_WINDOW] sekundi. */
export const CLIENT_LIMIT = 5;
export const CLIENT_WINDOW = 15 * 60;
/** Koliko krivih PIN-ova smije doći ukupno u [GLOBAL_WINDOW] sekundi, za napad s mnogo adresa. */
export const GLOBAL_LIMIT = 50;
export const GLOBAL_WINDOW = 60 * 60;

/** Usporedba neovisna o duljini poklapanja, da se PIN ne može pogađati po vremenu. */
export function pinMatches(supplied: string, expected: string): boolean {
  const a = new TextEncoder().encode(supplied);
  const b = new TextEncoder().encode(expected);
  let diff = a.length ^ b.length;
  const length = Math.max(a.length, b.length);
  for (let i = 0; i < length; i++) {
    diff |= (a[i] ?? 0) ^ (b[i] ?? 0);
  }
  return diff === 0;
}

/**
 * Čitanje je otvoreno svima; svaka izmjena traži ispravan PIN u zaglavlju `x-spy-pin`.
 * Vraća odgovor s greškom ili `null` ako je PIN u redu.
 *
 * Nakon previše krivih pokušaja odgovor je `429` i za ispravan PIN, da zaključavanje ne
 * otkrije koji je PIN točan.
 */
export async function requirePin(request: Request, env: Env): Promise<Response | null> {
  if (!env.ADMIN_PIN) return fail("pin_not_configured", 503);

  const client = request.headers.get("cf-connecting-ip") ?? "unknown";
  const recent = await env.DB.prepare(
    `SELECT (SELECT COUNT(*) FROM pin_failures WHERE client = ?1 AND at > unixepoch() - ?2) AS mine,
            (SELECT COUNT(*) FROM pin_failures WHERE at > unixepoch() - ?3) AS everyone`
  )
    .bind(client, CLIENT_WINDOW, GLOBAL_WINDOW)
    .first<{ mine: number; everyone: number }>();
  if (recent && (recent.mine >= CLIENT_LIMIT || recent.everyone >= GLOBAL_LIMIT)) {
    const wait = recent.mine >= CLIENT_LIMIT ? CLIENT_WINDOW : GLOBAL_WINDOW;
    return fail("too_many_attempts", 429, {}, { "retry-after": String(wait) });
  }

  const supplied = request.headers.get("x-spy-pin") ?? "";
  if (!pinMatches(supplied, env.ADMIN_PIN)) {
    await env.DB.batch([
      env.DB.prepare("INSERT INTO pin_failures (client) VALUES (?)").bind(client),
      env.DB.prepare("DELETE FROM pin_failures WHERE at <= unixepoch() - ?").bind(GLOBAL_WINDOW),
    ]);
    return fail("unauthorized", 401);
  }

  return null;
}
