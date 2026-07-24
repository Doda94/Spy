import type { Word } from "./words";

export type ApiErrorCode =
  | "unauthorized"
  | "pin_not_configured"
  | "duplicate"
  | "empty"
  | "too-long"
  | "not_found"
  | "offline"
  | "server";

export class ApiError extends Error {
  code: ApiErrorCode;
  constructor(code: ApiErrorCode) {
    super(code);
    this.name = "ApiError";
    this.code = code;
  }
}

async function errorFrom(response: Response): Promise<ApiError> {
  let code: ApiErrorCode = "server";
  try {
    const body = (await response.json()) as { error?: string };
    const known: ApiErrorCode[] = [
      "unauthorized",
      "pin_not_configured",
      "duplicate",
      "empty",
      "too-long",
      "not_found",
    ];
    if (body.error && (known as string[]).includes(body.error)) {
      code = body.error as ApiErrorCode;
    }
  } catch {
    /* tijelo nije JSON — ostaje "server" */
  }
  return new ApiError(code);
}

function pinHeaders(pin: string): HeadersInit {
  return { "content-type": "application/json", "x-spy-pin": pin };
}

export async function fetchWords(): Promise<Word[]> {
  let response: Response;
  try {
    response = await fetch("/api/words", { cache: "no-store" });
  } catch {
    throw new ApiError("offline");
  }
  if (!response.ok) throw await errorFrom(response);
  const body = (await response.json()) as { words?: Word[] };
  return body.words ?? [];
}

export async function verifyPin(pin: string): Promise<void> {
  let response: Response;
  try {
    response = await fetch("/api/auth", { method: "POST", headers: pinHeaders(pin) });
  } catch {
    throw new ApiError("offline");
  }
  if (!response.ok) throw await errorFrom(response);
}

export async function addWord(text: string, pin: string): Promise<Word> {
  let response: Response;
  try {
    response = await fetch("/api/words", {
      method: "POST",
      headers: pinHeaders(pin),
      body: JSON.stringify({ text }),
    });
  } catch {
    throw new ApiError("offline");
  }
  if (!response.ok) throw await errorFrom(response);
  const body = (await response.json()) as { word: Word };
  return body.word;
}

export async function deleteWord(id: number, pin: string): Promise<void> {
  let response: Response;
  try {
    response = await fetch(`/api/words/${id}`, {
      method: "DELETE",
      headers: pinHeaders(pin),
    });
  } catch {
    throw new ApiError("offline");
  }
  if (!response.ok) throw await errorFrom(response);
}
