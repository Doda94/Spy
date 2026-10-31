export type Env = {
  DB: D1Database;
  /** Zajednički PIN za uređivanje; postavlja se kao secret (`wrangler secret put ADMIN_PIN`). */
  ADMIN_PIN?: string;
};

/** Obična zaglavlja; namjerno ne `HeadersInit`, jer se spajaju razmazivanjem objekta. */
export type HeaderMap = Record<string, string>;

export const NO_STORE: HeaderMap = { "cache-control": "no-store" };

export function json(body: unknown, status = 200, headers: HeaderMap = NO_STORE): Response {
  return Response.json(body, { status, headers: { ...NO_STORE, ...headers } });
}

export function fail(
  code: string,
  status: number,
  extra: Record<string, unknown> = {},
  headers: HeaderMap = NO_STORE
): Response {
  return json({ error: code, ...extra }, status, headers);
}

/** Slaba ETag oznaka, jer Cloudflare može sažeti odgovor i time ionako oslabiti jaku. */
export function weakEtag(value: string): string {
  return `W/"${value}"`;
}

/**
 * `If-None-Match` s popisom oznaka ili `*`; uspoređuje se slabo, kao što traži RFC 9110.
 * Oznake se čitaju po navodnicima, a ne dijele po zarezu, jer zarez smije biti u oznaci.
 */
export function etagMatches(header: string | null, etag: string): boolean {
  if (!header) return false;
  if (header.trim() === "*") return true;
  const wanted = etag.replace(/^W\//, "");
  const tags = header.match(/(?:W\/)?"[^"]*"/g) ?? [];
  return tags.some((tag) => tag.replace(/^W\//, "") === wanted);
}

/**
 * JSON odgovor s ETagom; ako klijent već ima tu verziju, 304 bez tijela.
 * `no-cache` znači "uvijek provjeri", a ETag čini tu provjeru jeftinom.
 */
export function cacheable(request: Request, body: unknown, etag: string): Response {
  const headers = { etag, "cache-control": "no-cache" };
  if (etagMatches(request.headers.get("if-none-match"), etag)) {
    return new Response(null, { status: 304, headers });
  }
  return json(body, 200, headers);
}
