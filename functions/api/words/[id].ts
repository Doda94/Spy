type Env = {
  DB: D1Database;
  ADMIN_PIN?: string;
};

const noStore = { "cache-control": "no-store" };

/** DELETE /api/words/:id — brisanje riječi (PIN provjerava _middleware). */
export const onRequestDelete: PagesFunction<Env, "id"> = async ({ env, params }) => {
  const raw = Array.isArray(params.id) ? params.id[0] : params.id;
  const id = Number(raw);

  if (!Number.isInteger(id) || id <= 0) {
    return Response.json({ error: "invalid_id" }, { status: 400, headers: noStore });
  }

  const result = await env.DB.prepare("DELETE FROM words WHERE id = ?").bind(id).run();

  if (result.meta.changes === 0) {
    return Response.json({ error: "not_found" }, { status: 404, headers: noStore });
  }

  return new Response(null, { status: 204, headers: noStore });
};
