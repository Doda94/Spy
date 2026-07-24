type Env = {
  DB: D1Database;
  ADMIN_PIN?: string;
};

/**
 * POST /api/auth — provjera PIN-a.
 * Ako je zahtjev došao dovde, _middleware je već potvrdio PIN, pa je posao
 * ove rute samo javiti aplikaciji da smije otključati uređivanje.
 */
export const onRequestPost: PagesFunction<Env> = async () =>
  Response.json({ ok: true }, { headers: { "cache-control": "no-store" } });
