import { fail, type Env } from "./http";

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
 */
export function requirePin(request: Request, env: Env): Response | null {
  if (!env.ADMIN_PIN) return fail("pin_not_configured", 503);

  const supplied = request.headers.get("x-spy-pin") ?? "";
  if (!pinMatches(supplied, env.ADMIN_PIN)) return fail("unauthorized", 401);

  return null;
}
