/**
 * Čitanje riječi je otvoreno svima — igrači moraju moći pokrenuti partiju.
 * Svaka izmjena (dodavanje, brisanje, provjera PIN-a) traži ispravan PIN.
 *
 * PIN se postavlja kao Cloudflare secret:
 *   npx wrangler pages secret put ADMIN_PIN
 */

type Env = {
  DB: D1Database;
  ADMIN_PIN?: string;
};

/** Usporedba neovisna o duljini poklapanja, da se PIN ne može pogađati po vremenu. */
function pinMatches(supplied: string, expected: string): boolean {
  const a = new TextEncoder().encode(supplied);
  const b = new TextEncoder().encode(expected);
  let diff = a.length ^ b.length;
  const length = Math.max(a.length, b.length);
  for (let i = 0; i < length; i++) {
    diff |= (a[i] ?? 0) ^ (b[i] ?? 0);
  }
  return diff === 0;
}

export const onRequest: PagesFunction<Env> = async (context) => {
  const { request, env } = context;

  if (request.method === "GET" || request.method === "HEAD") {
    return context.next();
  }

  if (!env.ADMIN_PIN) {
    return Response.json(
      { error: "pin_not_configured" },
      { status: 503, headers: { "cache-control": "no-store" } }
    );
  }

  const supplied = request.headers.get("x-spy-pin") ?? "";
  if (!pinMatches(supplied, env.ADMIN_PIN)) {
    return Response.json(
      { error: "unauthorized" },
      { status: 401, headers: { "cache-control": "no-store" } }
    );
  }

  return context.next();
};
