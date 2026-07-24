import {
  DEFAULT_SETTINGS,
  MAX_PLAYERS,
  MIN_PLAYERS,
  type Game,
  type Phase,
  type SavedState,
  type Settings,
} from "./types";
import type { Word } from "./words";

export const GAME_STATE_KEY = "spy.gameState";
/** Zadnji uspješno dohvaćen popis — da se može igrati i bez mreže. */
export const WORDS_CACHE_KEY = "spy.wordsCache";
export const PIN_KEY = "spy.pin";

const PHASES: Phase[] = ["home", "setup", "reveal", "discussion"];

/* -------------------------------------------------------------------------- */
/*  Spremljeni popis riječi                                                   */
/* -------------------------------------------------------------------------- */

export function loadCachedWords(): Word[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(WORDS_CACHE_KEY);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (w): w is Word =>
        typeof w === "object" &&
        w !== null &&
        typeof (w as Word).id === "number" &&
        typeof (w as Word).text === "string" &&
        (w as Word).text.length > 0
    );
  } catch {
    return [];
  }
}

export function saveCachedWords(words: Word[]): void {
  writeKey(WORDS_CACHE_KEY, JSON.stringify(words));
}

/* -------------------------------------------------------------------------- */
/*  PIN                                                                       */
/* -------------------------------------------------------------------------- */

export function loadPin(): string {
  if (typeof window === "undefined") return "";
  try {
    return window.localStorage.getItem(PIN_KEY) ?? "";
  } catch {
    return "";
  }
}

export function savePin(pin: string): void {
  writeKey(PIN_KEY, pin);
}

export function clearPin(): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(PIN_KEY);
  } catch {
    /* ignoriramo */
  }
}

/* -------------------------------------------------------------------------- */
/*  Stanje igre                                                               */
/* -------------------------------------------------------------------------- */

function hasValidSettings(value: unknown): boolean {
  if (typeof value !== "object" || value === null) return false;
  const s = value as Record<string, unknown>;
  return (
    typeof s.playerCount === "number" &&
    Number.isInteger(s.playerCount) &&
    s.playerCount >= MIN_PLAYERS &&
    s.playerCount <= MAX_PLAYERS &&
    typeof s.spyCount === "number" &&
    Number.isInteger(s.spyCount) &&
    s.spyCount >= 1 &&
    s.spyCount < (s.playerCount as number)
  );
}

function isValidGame(value: unknown): value is Game {
  if (!hasValidSettings(value)) return false;
  const g = value as Record<string, unknown>;
  if (typeof g.word !== "string" || g.word.length === 0) return false;
  if (!Array.isArray(g.spyIndices)) return false;
  const playerCount = g.playerCount as number;
  const spiesOk = g.spyIndices.every(
    (i) => typeof i === "number" && Number.isInteger(i) && i >= 0 && i < playerCount
  );
  if (!spiesOk || g.spyIndices.length !== g.spyCount) return false;
  return (
    typeof g.currentPlayer === "number" &&
    Number.isInteger(g.currentPlayer) &&
    g.currentPlayer >= 0 &&
    g.currentPlayer <= playerCount
  );
}

export function loadState(): SavedState | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(GAME_STATE_KEY);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed !== "object" || parsed === null) return null;
    const s = parsed as Record<string, unknown>;

    const phase =
      typeof s.phase === "string" && PHASES.includes(s.phase as Phase)
        ? (s.phase as Phase)
        : "home";
    const settings = hasValidSettings(s.settings)
      ? (s.settings as Settings)
      : DEFAULT_SETTINGS;
    const game = isValidGame(s.game) ? s.game : null;

    // Faze koje traže aktivnu igru nemaju smisla bez nje.
    if ((phase === "reveal" || phase === "discussion") && !game) {
      return { phase: "home", settings, game: null };
    }

    return { phase, settings, game };
  } catch {
    return null;
  }
}

export function saveState(state: SavedState): void {
  writeKey(GAME_STATE_KEY, JSON.stringify(state));
}

function writeKey(key: string, value: string): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, value);
  } catch {
    /* npr. pun localStorage ili privatni način rada — igra i dalje radi */
  }
}
